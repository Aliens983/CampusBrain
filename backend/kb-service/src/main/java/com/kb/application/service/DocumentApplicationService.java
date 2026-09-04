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
import com.kb.infrastructure.tenant.TenantContext;
import com.kb.infrastructure.persistence.elasticsearch.EsDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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
 * Upload → MinIO → MySQL → RabbitMQ → Async Processing
 * </p>
 * @author forever-king
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentApplicationService implements IDocumentApplicationService {

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

        // 2. 保存文档元信息到 MySQL
        Document doc = Document.builder()
                .title(originalFilename)
                .fileType(fileType)
                .fileSize(file.getSize())
                .filePath(localPath)
                .status(DocumentStatus.UPLOADED)
                .ownerId(SecurityFrameworkUtils.getLoginUserId())
                .tenantId(TenantContext.getTenantId())
                .chunkCount(0)
                .build();
        Document saved = documentRepository.save(doc);

        // 3. Send async processing message to RabbitMQ
        mqProducer.send(saved.getId());

        log.info("Document uploaded: id={}, name={}, type={}, size={}",
                saved.getId(), originalFilename, fileType, file.getSize());

        metrics.recordDocumentUpload();

        return saved.getId();
    }

    /**
     * Get document by ID（仅本人可访问）
     */
    public Document getDocument(Long id) {
        return getOwnedDocument(id);
    }

    /**
     * Get all documents.
     */
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    /**
     * 删除文档及所有关联数据（MySQL + 本地文件 + Qdrant向量 + ES索引）
     */
    @Transactional
    public void deleteDocument(Long id) {
        Document doc = getOwnedDocument(id);
        String docIdStr = String.valueOf(id);

        // 1. 删除本地文件
        deleteLocalFile(doc.getFilePath());

        // 2. 删除 Qdrant 向量
        try {
            vectorStore.deleteByDocumentId(docIdStr);
        } catch (Exception e) {
            log.warn("Qdrant delete failed for document: id={}", id, e);
        }

        // 3. 删除 ES 索引
        try {
            esRepository.deleteByDocumentId(docIdStr);
        } catch (Exception e) {
            log.warn("ES delete failed for document: id={}", id, e);
        }

        // 4. 删除 MySQL 记录（文档 + 分块级联）
        documentRepository.delete(id);
        log.info("Document fully deleted: id={}", id);
    }

    /**
     * Get document processing status.
     */
    public DocumentStatus getDocumentStatus(Long id) {
        return getOwnedDocument(id).getStatus();
    }

    /**
     * 获取并校验文档归属：仅文档 owner 本人可访问，否则视为不存在
     */
    private Document getOwnedDocument(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException.DocumentException(
                        ErrorCode.DOCUMENT_NOT_FOUND, "id=" + id));
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        if (currentUserId == null || !currentUserId.equals(doc.getOwnerId())) {
            throw new BusinessException.DocumentException(
                    ErrorCode.DOCUMENT_NOT_FOUND, "id=" + id);
        }
        return doc;
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

    /**
     * 删除本地文件
     */
    private void deleteLocalFile(String filePath) {
        if (filePath == null) return;
        try {
            Files.deleteIfExists(Paths.get(filePath));
            log.info("Local file deleted: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete local file: {}", filePath, e);
        }
    }

    private String extractFileType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
