package com.kb.infrastructure.mq;

import com.kb.domain.document.Document;
import com.kb.domain.document.DocumentChunk;
import com.kb.domain.document.DocumentRepository;
import com.kb.domain.document.DocumentStatus;
import com.kb.domain.event.DocumentProcessedEvent;
import com.kb.domain.rag.*;
import com.kb.infrastructure.persistence.elasticsearch.EsDocumentEntity;
import com.kb.infrastructure.persistence.elasticsearch.EsDocumentRepository;
import com.kb.infrastructure.rag.chunker.ChunkerFactory;
import com.kb.infrastructure.rag.graph.KnowledgeGraphService;
import com.kb.infrastructure.rag.parser.ParserChain;
import com.kb.infrastructure.rag.parser.ParserFactory;
import com.kb.infrastructure.schedule.DocumentProcessingLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Consumer: processes documents asynchronously from RabbitMQ.
 * <p>
 * Full ingestion pipeline:
 * <ol>
 *   <li>从本地存储目录读取文件（文档保存目录由 app.file-storage-path 指定）</li>
 *   <li>Parse: extract clean text via Tika</li>
 *   <li>Chunk: split into segments</li>
 *   <li>Embed: convert chunks to vectors</li>
 *   <li>Store: persist to Qdrant (vectors) + ES (keywords)</li>
 * </ol>
 * </p>
 * <p>
 * <b>失败语义（12-03）</b>：
 * <ul>
 *   <li>永久性失败（文件丢失、解析不支持/文件损坏）→ 置 FAILED 并 ACK，不做无意义重试；</li>
 *   <li>可重试失败（embedding/Qdrant/ES/DB 抖动）→ 向外抛出，由 Spring retry 退避重试 3 次，
 *       仍失败则经死信交换机进入 DLQ 留存，文档保留中间态由超时回收任务重新投递。</li>
 * </ul>
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingConsumer {

    /** 文档领域仓储 */
    private final DocumentRepository documentRepository;

    /** 文档解析器工厂 */
    private final ParserFactory parserFactory;

    /** 解析器责任链（主解析器 + fallback） */
    private final ParserChain parserChain;

    /** 分块器工厂 */
    private final ChunkerFactory chunkerFactory;

    /** 向量嵌入服务 */
    private final EmbeddingService embeddingService;

    /** 向量存储服务 */
    private final VectorStoreService vectorStore;

    /** Elasticsearch文档仓储 */
    private final EsDocumentRepository esRepository;

    /** 事件发布器 */
    private final ApplicationEventPublisher eventPublisher;

    /** 知识图谱服务 — 文档入库时自动抽取实体 */
    private final KnowledgeGraphService kgService;

    /** 处理中/回收重投的 Redis 短锁：防止重试与超时重投造成并发处理同一文档 */
    private final DocumentProcessingLock processingLock;

    /** 分块批量写入的单事务边界（A-03） */
    private final com.kb.infrastructure.persistence.mysql.DocumentChunkTransactionService chunkTransactionService;

    /** 文档分块策略 */
    @Value("${chunking.strategy}")
    private String chunkStrategy;

    /**
     * 永久性处理失败：重试无意义（文件丢失/损坏/不支持的格式）。
     * 捕获后置 FAILED 并正常 ACK，消息不进 DLQ。
     */
    public static class PermanentProcessingException extends RuntimeException {
        public PermanentProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Consume document processing messages from the queue.
     */
    @RabbitListener(queues = "kb.document.processing.queue")
    public void processDocument(DocumentProcessingMessage message) {
        Long documentId = message.getDocumentId();
        log.info("Processing document: id={}, forceReprocess={}",
                documentId, message.isForceReprocess());

        Optional<Document> docOpt = documentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            // 文档行已不存在（删除/回滚）：直接 ACK，重投也处理不了
            log.warn("Document not found, ack and skip: id={}", documentId);
            return;
        }

        Document doc = docOpt.get();

        // Skip already-processed documents unless forced
        if (doc.isReady() && !message.isForceReprocess()) {
            log.info("Document already processed: id={}", documentId);
            return;
        }

        // 并发保护：enterProcessing 通过"Redis token + 本机在途登记表"双重判据（3.9）识别
        // "本地重试 / TTL 内回到本实例的重投递"（放行）与"重复消息/其他实例正在处理"（ACK 跳过），
        // 杜绝两个消费者并发执行"删旧数据→写新数据"。
        if (!processingLock.enterProcessing(documentId)) {
            log.info("文档正在被其他消费者处理，本次消息直接确认跳过: id={}", documentId);
            return;
        }

        try {
            // Step 0: 幂等——处理前清理该文档的旧向量/ES 索引/分块，避免失败重试时重复写入
            String docIdStr = String.valueOf(documentId);
            try {
                vectorStore.deleteByDocumentId(docIdStr);
                esRepository.deleteByDocumentId(docIdStr);
                documentRepository.deleteChunksByDocumentId(documentId);
                // 内存知识图谱同步摘除旧归属，避免重处理后旧实体/旧边残留（12-10）
                kgService.deleteByDocument(documentId);
            } catch (Exception e) {
                log.warn("清理旧数据失败（继续处理）: id={}", documentId, e);
            }

            // === Step 1: Parse（解析失败视为永久性失败：损坏/加密/不支持的文件不会因重试变好）===
            updateStatus(doc, DocumentStatus.PARSING);
            ParsedDocument parsed;
            try {
                parsed = parse(doc);
            } catch (Exception e) {
                throw new PermanentProcessingException(
                        "文档解析失败（文件损坏、加密或格式不支持）: id=" + documentId, e);
            }
            log.info("Parsed document: id={}, chars={}", documentId, parsed.getCharCount());

            // === Step 2: Chunk ===
            updateStatus(doc, DocumentStatus.CHUNKING);
            List<DocumentChunk> chunks = chunk(parsed);
            log.info("Chunked document: id={}, chunks={}", documentId, chunks.size());

            // === Step 3: Embed（失败可重试：模型限流/超时）===
            updateStatus(doc, DocumentStatus.EMBEDDING);
            List<float[]> embeddings = embed(chunks);

            // === Step 3.5: Build knowledge graph（best-effort：
            // 图谱是检索增强能力，抽取失败不能拖垮文档入库主链路，详见 12-10）===
            for (DocumentChunk chunk : chunks) {
                if (chunk.getContent() != null && !chunk.getContent().isBlank()) {
                    try {
                        kgService.ingestChunk(chunk.getContent(), documentId, chunk.getQdrantId());
                    } catch (Exception kgEx) {
                        log.warn("知识图谱抽取失败，跳过该分块（不影响文档入库）: id={}, chunk={}",
                                documentId, chunk.getQdrantId(), kgEx);
                    }
                }
            }

            // === Step 4: Save chunks to MySQL first（让 chunk 拿到 documentId）===
            // A-03：整批分块在单个事务内写入，中途失败整体回滚，不留半成品分块
            chunkTransactionService.saveChunksAtomically(chunks, documentId);

            // === Step 5: Store (Qdrant + ES) — 用正确的 documentId（失败可重试）===
            // 4.1（深度审查 P0）：写入归属用户 ownerId，检索端据此过滤
            store(chunks, embeddings, documentId, doc.getOwnerId());

            // === Step 6: Mark READY ===
            documentRepository.markReady(documentId, chunks.size());
            log.info("Document processing complete: id={}, chunks={}",
                    documentId, chunks.size());

            eventPublisher.publishEvent(new DocumentProcessedEvent(
                    this, documentId, doc.getTitle(), doc.getOwnerId(),
                    DocumentStatus.READY, null));
            // 成功终态：释放处理锁
            processingLock.exitProcessing(documentId);

        } catch (PermanentProcessingException permanent) {
            // 永久失败：置 FAILED 并正常 ACK（不重试、不进 DLQ）
            log.error("Document processing permanently failed: id={}", documentId, permanent);
            String errMsg = truncate(permanent.getMessage(), 500);
            documentRepository.updateStatus(documentId, DocumentStatus.FAILED, errMsg);
            eventPublisher.publishEvent(new DocumentProcessedEvent(
                    this, documentId, doc.getTitle(), doc.getOwnerId(),
                    DocumentStatus.FAILED, errMsg));
            processingLock.exitProcessing(documentId);

        } catch (Exception retryable) {
            // 可重试失败：保持当前中间态向外抛出 ——
            // Spring retry 在本实例退避重试 max-attempts 次；
            // 仍失败则被拒绝并路由到 DLQ（default-requeue-rejected=false + 队列 DLX 参数）。
            // 不在这里把文档改成 FAILED：中间态 + updated_at 时间戳让超时回收任务能发现并重新投递。
            // 同时不释放 Redis 处理锁：TTL 窗口内重投的消息（含回到本实例）虽可被 owner 识别，
            // 但跨实例重复消费仍被锁挡住，给下游（embedding/Qdrant）留出恢复窗口。
            log.error("Document processing failed (retryable), will retry/DLQ: id={}",
                    documentId, retryable);
            if (retryable instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("文档处理失败（可重试）: id=" + documentId, retryable);
        } finally {
            // 3.9：注销本机在途登记（终态路径 exitProcessing 已移除，幂等）；
            // 可重试路径在此取消"在途"标记，Spring retry 下一轮（token 仍在 Redis）可重新入场
            processingLock.markTaskFinished(documentId);
        }
    } // end processDocument

    // ========== Pipeline Steps ==========

    private ParsedDocument parse(Document doc) throws Exception {
        try (InputStream fileStream = new java.io.FileInputStream(doc.getFilePath())) {
            // Use ParserChain: tries primary parser first, auto-fallbacks on failure
            return parserChain.parseAuto(fileStream, doc.getTitle());
        } catch (FileNotFoundException fnf) {
            // 4.17（深度审查 P2）：错误信息不得携带服务器绝对路径——
            // 该 message 会写入 error_msg 并经 DocumentStatusNotifier 推送到前端
            throw new PermanentProcessingException(
                    "原始文件不存在，可能已被外部清理（文件 ID=" + doc.getId() + "）", fnf);
        }
    }

    private List<DocumentChunk> chunk(ParsedDocument parsed) {
        ChunkStrategy chunker = chunkerFactory.getStrategy(chunkStrategy);
        Map<String, Object> chunkMeta = new HashMap<>();
        chunkMeta.put("fileType", parsed.getFileType());
        if (parsed.getMetadata() != null) {
            chunkMeta.putAll(parsed.getMetadata());
        }
        return chunker.chunk(parsed.getContent(), chunkMeta);
    }

    private List<float[]> embed(List<DocumentChunk> chunks) {
        List<String> texts = chunks.stream()
                .map(DocumentChunk::getContent)
                .toList();
        List<float[]> embeddings = embeddingService.embedBatch(texts);

        // Assign Qdrant point IDs
        for (int i = 0; i < chunks.size(); i++) {
            String pointId = UUID.randomUUID().toString();
            chunks.get(i).assignQdrantId(pointId);
        }
        return embeddings;
    }

    private void store(List<DocumentChunk> chunks, List<float[]> embeddings, Long documentId, Long ownerId) {
        List<VectorStoreService.VectorPoint> qdrantPoints = new ArrayList<>();
        List<EsDocumentEntity> esDocs = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            float[] vector = embeddings.get(i);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("chunk_id", chunk.getQdrantId());
            payload.put("document_id", String.valueOf(documentId));
            payload.put("document_title", chunk.getMetadata() != null
                    ? chunk.getMetadata().getOrDefault("documentTitle", "") : "");
            payload.put("chunk_index", chunk.getChunkIndex());
            payload.put("content", chunk.getContent());
            payload.put("section_title", chunk.getMetadata() != null
                    ? chunk.getMetadata().getOrDefault("sectionTitle", "") : "");
            // 4.1（深度审查 P0）：写入归属用户，检索端据此过滤跨用户私有泄露；null 视为全局共享
            if (ownerId != null) {
                payload.put("owner_id", String.valueOf(ownerId));
            }

            qdrantPoints.add(new VectorStoreService.VectorPoint(
                    chunk.getQdrantId(), vector, payload));

            esDocs.add(EsDocumentEntity.builder()
                    .chunkId(chunk.getQdrantId())
                    .documentId(String.valueOf(documentId))
                    .documentTitle((String) payload.get("document_title"))
                    .content(chunk.getContent())
                    .chunkIndex(chunk.getChunkIndex())
                    .createdAt(LocalDateTime.now()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .ownerId(ownerId)
                    .build());
        }

        // Batch write
        vectorStore.upsert(qdrantPoints);
        esRepository.bulkIndex(esDocs);
    }

    // ========== Helpers ==========

    private void updateStatus(Document doc, DocumentStatus status) {
        documentRepository.updateStatus(doc.getId(), status, null);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}
