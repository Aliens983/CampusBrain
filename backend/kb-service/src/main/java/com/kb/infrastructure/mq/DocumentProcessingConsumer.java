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
import com.kb.infrastructure.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Consumer: processes documents asynchronously from RabbitMQ.
 * <p>
 * Full ingestion pipeline:
 * <ol>
 *   <li>Fetch file from MinIO</li>
 *   <li>Parse: extract clean text via Tika</li>
 *   <li>Chunk: split into segments</li>
 *   <li>Embed: convert chunks to vectors</li>
 *   <li>Store: persist to Qdrant (vectors) + ES (keywords)</li>
 * </ol>
 * </p>
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

    /** 文档分块策略 */
    @Value("${chunking.strategy}")
    private String chunkStrategy;

    /**
     * Consume document processing messages from the queue.
     */
    @RabbitListener(queues = "kb.document.processing.queue")
    public void processDocument(DocumentProcessingMessage message) {
        Long documentId = message.getDocumentId();

        // 恢复租户上下文（从生产者线程的 HTTP 请求传递到消费者线程）
        if (message.getTenantId() != null) {
            TenantContext.setTenant(message.getTenantId());
        }

        try {
            processDocumentInternal(message);
        } finally {
            TenantContext.clear();
        }
    }

    private void processDocumentInternal(DocumentProcessingMessage message) {
        Long documentId = message.getDocumentId();
        log.info("Processing document: id={}, tenantId={}, forceReprocess={}",
                documentId, message.getTenantId(), message.isForceReprocess());

        Optional<Document> docOpt = documentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            log.error("Document not found: id={}", documentId);
            return;
        }

        Document doc = docOpt.get();

        // Skip already-processed documents unless forced
        if (doc.isReady() && !message.isForceReprocess()) {
            log.info("Document already processed: id={}", documentId);
            return;
        }

        try {
            // Step 0: 幂等——处理前清理该文档的旧向量/ES 索引/分块，避免失败重试时重复写入
            String docIdStr = String.valueOf(documentId);
            try {
                vectorStore.deleteByDocumentId(docIdStr);
                esRepository.deleteByDocumentId(docIdStr);
                documentRepository.deleteChunksByDocumentId(documentId);
            } catch (Exception e) {
                log.warn("清理旧数据失败（继续处理）: id={}", documentId, e);
            }

            // === Step 1: Parse ===
            updateStatus(doc, DocumentStatus.PARSING);
            ParsedDocument parsed = parse(doc);
            log.info("Parsed document: id={}, chars={}", documentId,
                    parsed.getCharCount());

            // === Step 2: Chunk ===
            updateStatus(doc, DocumentStatus.CHUNKING);
            List<DocumentChunk> chunks = chunk(parsed);
            log.info("Chunked document: id={}, chunks={}", documentId, chunks.size());

            // === Step 3: Embed ===
            updateStatus(doc, DocumentStatus.EMBEDDING);
            List<float[]> embeddings = embed(chunks);

            // === Step 3.5: Build knowledge graph（实体抽取 + 关系构建）
            for (DocumentChunk chunk : chunks) {
                if (chunk.getContent() != null && !chunk.getContent().isBlank()) {
                    kgService.ingestChunk(chunk.getContent(), documentId, chunk.getQdrantId());
                }
            }

            // === Step 4: Save chunks to MySQL first（让 chunk 拿到 documentId）
            documentRepository.saveChunks(chunks, documentId);

            // === Step 5: Store (Qdrant + ES) — 用正确的 documentId
            store(chunks, embeddings, documentId);

            // === Step 6: Mark READY ===
            documentRepository.markReady(documentId, chunks.size());
            log.info("Document processing complete: id={}, chunks={}",
                    documentId, chunks.size());

            eventPublisher.publishEvent(new DocumentProcessedEvent(
                    this, documentId, doc.getTitle(), doc.getOwnerId(),
                    DocumentStatus.READY, null));

        } catch (Exception e) {
            log.error("Document processing failed: id={}", documentId, e);
            String errMsg = truncate(e.getMessage(), 500);
            documentRepository.updateStatus(documentId, DocumentStatus.FAILED, errMsg);

            eventPublisher.publishEvent(new DocumentProcessedEvent(
                    this, documentId, doc.getTitle(), doc.getOwnerId(),
                    DocumentStatus.FAILED, errMsg));
        }
    } // end processDocumentInternal

    // ========== Pipeline Steps ==========

    private ParsedDocument parse(Document doc) throws Exception {
        try (InputStream fileStream = new java.io.FileInputStream(doc.getFilePath())) {
            // Use ParserChain: tries primary parser first, auto-fallbacks on failure
            return parserChain.parseAuto(fileStream, doc.getTitle());
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

    private void store(List<DocumentChunk> chunks, List<float[]> embeddings, Long documentId) {
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
            payload.put("tenant_id", TenantContext.getTenantId());

            qdrantPoints.add(new VectorStoreService.VectorPoint(
                    chunk.getQdrantId(), vector, payload));

            esDocs.add(EsDocumentEntity.builder()
                    .chunkId(chunk.getQdrantId())
                    .documentId(String.valueOf(documentId))
                    .documentTitle((String) payload.get("document_title"))
                    .content(chunk.getContent())
                    .chunkIndex(chunk.getChunkIndex())
                    .tenantId(TenantContext.getTenantId())
                    .createdAt(LocalDateTime.now()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
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
