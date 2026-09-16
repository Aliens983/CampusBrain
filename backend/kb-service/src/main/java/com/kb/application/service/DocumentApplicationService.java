package com.kb.application.service;

import com.kb.domain.document.Document;
import com.kb.domain.document.DocumentRepository;
import com.kb.domain.document.DocumentStatus;
import com.kb.domain.rag.VectorStoreService;
import com.kb.infrastructure.common.BusinessException;
import com.kb.infrastructure.common.ErrorCode;
import com.kb.infrastructure.metrics.BusinessMetrics;
import com.kb.infrastructure.mq.DocumentProcessingProducer;
import com.kb.infrastructure.rag.parser.ParserRegistry;
import com.kb.infrastructure.security.SecurityFrameworkUtils;
import com.kb.infrastructure.persistence.elasticsearch.EsDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * Application service for document lifecycle management.
 * <p>
 * Coordinates the document upload workflow:
 * Local Disk → MySQL → RabbitMQ (after commit) → Async Processing
 * </p>
 * @author forever-king
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentApplicationService implements IDocumentApplicationService {

    /** 默认页码（0 基） */
    private static final int DEFAULT_PAGE = 0;
    /** 默认每页条数 */
    private static final int DEFAULT_SIZE = 20;
    /** 每页最大条数，防止一次拉取全表 */
    private static final int MAX_SIZE = 100;
    /** 外部存储清理（向量/索引/文件）的最大尝试次数（含首次） */
    private static final int CLEANUP_MAX_ATTEMPTS = 3;
    /** 清理重试退避（毫秒），随尝试次数倍增 */
    private static final long CLEANUP_BACKOFF_MS = 200L;

    /** 文档仓库接口 */
    private final DocumentRepository documentRepository;

    /** 消息队列生产者，触发异步文档处理 */
    private final DocumentProcessingProducer mqProducer;

    /** 向量存储服务，用于删除文档时清理向量 */
    private final VectorStoreService vectorStore;

    /** Elasticsearch文档仓储，用于删除文档时清理索引 */
    private final EsDocumentRepository esRepository;

    /** 业务指标收集器 */
    private final BusinessMetrics metrics;

    /** 解析器注册中心（上传文件类型校验以此为准） */
    private final ParserRegistry parserRegistry;

    /** 本地文件存储目录 */
    @Value("${app.file-storage-path:./file}")
    private String fileStoragePath;

    /**
     * Upload a document and trigger async processing.
     * <p>
     * 顺序：先落盘 → 再写 DB（事务）→ 事务提交后才发 MQ。
     * 落盘之后任一步失败都补偿删除本地文件，避免无主文件；
     * MQ 在提交后发送，避免消费者在旧事务未提交时查不到文档行。
     *
     * @param file the uploaded multipart file
     * @return the created document ID
     */
    @Transactional
    public Long uploadDocument(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String fileType = extractFileType(originalFilename);

        // 1. 文件类型校验：以解析器注册表为准（有解析器才能处理），
        //    避免"上传时白名单放行、异步处理时才报不支持"的错位。
        if (parserRegistry.getParserChain(fileType).isEmpty()) {
            String supported = String.join(" / ", parserRegistry.supportedExtensions());
            throw new BusinessException.DocumentException(
                    ErrorCode.DOCUMENT_UNSUPPORTED_TYPE,
                    fileType + "（当前支持：" + supported + "）");
        }

        // 2. 安全文件名：仅 UUID + 合法扩展名，避免路径穿越/任意文件写入
        String fileName = UUID.randomUUID() + "." + fileType;
        String localPath = saveToLocal(file, fileName);

        // 3. 元信息落库 + 提交后投递 MQ；落盘之后的任何失败都必须回删文件
        try {
            Document doc = Document.builder()
                    .title(originalFilename)
                    .fileType(fileType)
                    .fileSize(file.getSize())
                    .filePath(localPath)
                    .status(DocumentStatus.UPLOADED)
                    .ownerId(SecurityFrameworkUtils.getLoginUserId())
                    .chunkCount(0)
                    .build();
            Document saved = documentRepository.save(doc);
            Long documentId = saved.getId();

            // 事务提交后再发消息：消费者按 id 回查 DB，提交前发送可能查不到行，
            // 也可能在事务回滚后白白触发一次失败消费
            afterCommit(() -> sendProcessingMessageWithRetry(documentId));

            log.info("Document uploaded: id={}, name={}, type={}, size={}",
                    documentId, originalFilename, fileType, file.getSize());

            metrics.recordDocumentUpload();
            return documentId;
        } catch (RuntimeException e) {
            // DB 落库失败（或注册回调等意外）：事务将随异常回滚，主动清理已落盘文件
            log.warn("文档元信息落库失败，补偿删除已上传文件: path={}", localPath, e);
            deleteLocalFileQuietly(localPath);
            throw e;
        }
    }

    /**
     * Get document by ID（仅本人可访问）
     */
    public Document getDocument(Long id) {
        return getOwnedDocument(id);
    }

    /**
     * 列出当前登录用户可见的文档：普通用户只看自己的，ADMIN 看全量（违规内容治理）。
     * 分页下推 SQL，不再无界加载。
     *
     * @param page 0 基页码
     * @param size 每页条数（1~100）
     * @return 当前用户有权查看的文档列表
     */
    public List<Document> listVisibleDocuments(int page, int size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (isAdmin()) {
            return documentRepository.findAll(normalizedPage, normalizedSize);
        }
        if (userId == null) {
            // 取不到身份时一律返回空，绝不退化成"全量可见"
            return List.of();
        }
        return documentRepository.findByOwnerId(userId, normalizedPage, normalizedSize);
    }

    /**
     * 按标题关键词搜索当前用户可见的文档。
     * <p>
     * 普通用户与管理员的模糊条件均下推到 SQL（{@code title LIKE}），
     * 不再把整表（含 filePath 等大字段）加载进 JVM 过滤；带分页与空标题保护。
     *
     * @param keyword 标题关键词
     * @param page    0 基页码
     * @param size    每页条数（1~100）
     * @return 命中的文档列表
     */
    public List<Document> searchVisibleDocuments(String keyword, int page, int size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (keyword == null || keyword.isBlank()) {
            return listVisibleDocuments(normalizedPage, normalizedSize);
        }
        if (isAdmin()) {
            return documentRepository.searchAllByTitle(keyword, normalizedPage, normalizedSize);
        }
        if (userId == null) {
            return List.of();
        }
        return documentRepository.searchByOwnerIdAndTitle(
                userId, keyword, normalizedPage, normalizedSize);
    }

    /** 当前登录用户是否为管理员 */
    private boolean isAdmin() {
        var loginUser = SecurityFrameworkUtils.getLoginUser();
        return loginUser != null && "ADMIN".equalsIgnoreCase(loginUser.getRole());
    }

    /**
     * 删除文档及所有关联数据（MySQL + Qdrant 向量 + ES 索引 + 本地文件）。
     * 权限：文档 owner 本人，或 ADMIN（违规内容治理）。
     * <p>
     * 顺序为"先删 DB（事务提交）→ 再清外部存储"：
     * 外部调用不再参与数据库事务，杜绝"存储已清、DB 回滚"造成的孤儿文档；
     * 向量/索引删除带有限重试，最终失败记 ERROR 并计入文档失败指标，不再静默吞掉。
     */
    @Transactional
    public void deleteDocument(Long id) {
        Document doc = getOwnedOrAdminDocument(id);

        // 1. 先删 DB（文档 + 分块级联），事务可能回滚，回滚时外部存储一律不动
        documentRepository.delete(id);

        // 2. 事务成功提交后清理向量、索引与本地文件
        String filePath = doc.getFilePath();
        afterCommit(() -> cleanupExternalStores(id, filePath));

        log.info("Document DB record deleted, external cleanup scheduled after commit: id={}", id);
    }

    /**
     * Get document processing status.
     */
    public DocumentStatus getDocumentStatus(Long id) {
        return getOwnedDocument(id).getStatus();
    }

    // ==================== 内部辅助 ====================

    /**
     * 事务提交后执行；若当前没有活动事务（如纯单测/非事务调用方），则立即执行。
     */
    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    /**
     * 清理文档的外部存储数据：向量索引 → ES 索引 → 本地文件。
     * 每项独立、带有限重试；单项最终失败不阻断其余清理，
     * 但必须以 ERROR 日志 + 失败指标暴露，供运维对账，绝不静默吞异常。
     */
    private void cleanupExternalStores(Long id, String filePath) {
        boolean vectorOk = runWithRetry("Qdrant 向量删除", id,
                () -> vectorStore.deleteByDocumentId(String.valueOf(id)));
        boolean esOk = runWithRetry("ES 索引删除", id,
                () -> esRepository.deleteByDocumentId(String.valueOf(id)));
        boolean fileOk = true;
        if (filePath != null && !filePath.isBlank()) {
            fileOk = runWithRetry("本地文件删除", id, () -> deleteLocalFileOrThrow(filePath));
        }
        if (!vectorOk || !esOk || !fileOk) {
            metrics.recordDocumentFailure();
            log.error("文档外部存储清理存在失败项，需人工/对账任务核查：id={}, vectorOk={}, esOk={}, fileOk={}",
                    id, vectorOk, esOk, fileOk);
        } else {
            log.info("Document fully deleted including external stores: id={}", id);
        }
    }

    /**
     * 有限次重试执行外部清理动作，异常退避后重试；达到上限返回 false（不抛出，
     * 因为事务已经提交，抛出无法改变结果，调用方负责记录与告警）。
     */
    private boolean runWithRetry(String actionName, Long documentId, Runnable action) {
        for (int attempt = 1; attempt <= CLEANUP_MAX_ATTEMPTS; attempt++) {
            try {
                action.run();
                return true;
            } catch (Exception e) {
                if (attempt >= CLEANUP_MAX_ATTEMPTS) {
                    log.error("{}在 {} 次尝试后仍失败: documentId={}",
                            actionName, CLEANUP_MAX_ATTEMPTS, documentId, e);
                    return false;
                }
                log.warn("{}第 {} 次尝试失败，将重试: documentId={}",
                        actionName, attempt, documentId, e);
                try {
                    Thread.sleep(CLEANUP_BACKOFF_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("{}等待重试时被中断: documentId={}", actionName, documentId, ie);
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 事务提交后投递文档处理消息，带有限重试；最终失败计入失败指标
     * （文档停留在 UPLOADED 状态，可由运维/对账任务重新投递）。
     */
    private void sendProcessingMessageWithRetry(Long documentId) {
        boolean sent = runWithRetry("文档处理 MQ 投递", documentId,
                () -> mqProducer.send(documentId));
        if (!sent) {
            metrics.recordDocumentFailure();
        }
    }

    private int normalizePage(int page) {
        return Math.max(page, DEFAULT_PAGE);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /**
     * 获取并校验文档归属：仅文档 owner 本人可访问，否则视为不存在
     */
    private Document getOwnedDocument(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException.DocumentException(
                        ErrorCode.DOCUMENT_NOT_FOUND, "id=" + id));
        if (!isOwner(doc, SecurityFrameworkUtils.getLoginUserId())) {
            throw new BusinessException.DocumentException(
                    ErrorCode.DOCUMENT_NOT_FOUND, "id=" + id);
        }
        return doc;
    }

    /**
     * 删除场景的归属校验：文档 owner 本人或 ADMIN 可访问，否则视为不存在。
     * 非属主统一抛 NOT_FOUND 而非 FORBIDDEN，避免文档存在性被枚举。
     */
    private Document getOwnedOrAdminDocument(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException.DocumentException(
                        ErrorCode.DOCUMENT_NOT_FOUND, "id=" + id));
        var loginUser = SecurityFrameworkUtils.getLoginUser();
        Long currentUserId = loginUser != null ? loginUser.getUserId() : null;
        boolean isAdmin = loginUser != null && "ADMIN".equalsIgnoreCase(loginUser.getRole());
        if (isAdmin || isOwner(doc, currentUserId)) {
            return doc;
        }
        throw new BusinessException.DocumentException(
                ErrorCode.DOCUMENT_NOT_FOUND, "id=" + id);
    }

    private boolean isOwner(Document doc, Long currentUserId) {
        return currentUserId != null && currentUserId.equals(doc.getOwnerId());
    }

    /**
     * 保存上传文件到本地 file 目录
     */
    private String saveToLocal(MultipartFile file, String fileName) {
        try {
            Path dir = Paths.get(fileStoragePath);
            if (!dir.isAbsolute()) {
                dir = Paths.get(System.getProperty("user.dir")).resolve(dir);
            }
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path targetPath = dir.resolve(fileName);
            file.transferTo(targetPath.toFile());
            log.info("File saved locally: {}", targetPath.toAbsolutePath());
            return targetPath.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file to local storage", e);
        }
    }

    /** 删除本地文件，失败抛异常（供重试逻辑感知） */
    private void deleteLocalFileOrThrow(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
            log.info("Local file deleted: {}", filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete local file: " + filePath, e);
        }
    }

    /** 补偿性删除本地文件（上传失败回滚场景），仅记录日志，不再向外抛 */
    private void deleteLocalFileQuietly(String filePath) {
        if (filePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            log.error("上传失败后补偿删除本地文件仍失败，可能残留无主文件: {}", filePath, e);
        }
    }

    private String extractFileType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
