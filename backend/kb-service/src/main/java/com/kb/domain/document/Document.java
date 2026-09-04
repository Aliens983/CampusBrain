package com.kb.domain.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Document aggregate root.
 * <p>
 * Represents an uploaded enterprise document going through the
 * RAG ingestion pipeline: Upload → Parse → Chunk → Embed → Ready.
 * </p>
 * @author forever-king
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    /** 文档唯一标识 */
    private Long id;

    /** 文档标题 */
    private String title;

    /** 文件类型（如 pdf、md、txt、xlsx 等） */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件存储路径 */
    private String filePath;

    /** 文档处理状态 */
    private DocumentStatus status;

    /** 文档被分割后的分块数量 */
    private Integer chunkCount;

    /** 文档所属用户ID */
    private Long ownerId;

    /** 所属租户 ID（多租户隔离） */
    private Long tenantId;

    /** 文档元数据（如作者、页数、创建日期等） */
    private Map<String, Object> metadata;

    /** 处理失败时的错误信息 */
    private String errorMsg;

    /** 文档创建时间 */
    private LocalDateTime createdAt;

    /** 文档最后更新时间 */
    private LocalDateTime updatedAt;

    // ========== Domain Behaviors ==========

    /**
     * Transition to PARSING status.
     */
    public void startParsing() {
        if (this.status != DocumentStatus.UPLOADED) {
            throw new IllegalStateException(
                    "Cannot parse document in status: " + this.status);
        }
        this.status = DocumentStatus.PARSING;
    }

    /**
     * Transition to CHUNKING status.
     */
    public void startChunking() {
        if (this.status != DocumentStatus.PARSING) {
            throw new IllegalStateException(
                    "Cannot chunk document in status: " + this.status);
        }
        this.status = DocumentStatus.CHUNKING;
    }

    /**
     * Transition to EMBEDDING status.
     */
    public void startEmbedding() {
        if (this.status != DocumentStatus.CHUNKING) {
            throw new IllegalStateException(
                    "Cannot embed document in status: " + this.status);
        }
        this.status = DocumentStatus.EMBEDDING;
    }

    /**
     * Mark document as READY (fully indexed and searchable).
     */
    public void markReady(int chunkCount) {
        this.status = DocumentStatus.READY;
        this.chunkCount = chunkCount;
    }

    /**
     * Mark document as FAILED with error details.
     */
    public void markFailed(String errorMessage) {
        this.status = DocumentStatus.FAILED;
        this.errorMsg = errorMessage;
    }

    /**
     * Whether this document is ready for Q&A retrieval.
     */
    public boolean isReady() {
        return this.status == DocumentStatus.READY;
    }

    /**
     * Add custom metadata key-value.
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
}
