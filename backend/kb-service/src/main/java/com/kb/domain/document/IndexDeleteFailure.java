package com.kb.domain.document;

import java.time.LocalDateTime;

/**
 * 外部索引删除失败对账记录（A-04）。
 * <p>
 * 同一 (documentId, target) 在表中只保留一行（唯一键），重复失败走重试次数累加。
 *
 * @author forever-king
 */
public class IndexDeleteFailure {

    private Long id;
    private final Long documentId;
    private final IndexTarget target;
    private String failReason;
    private int retryCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public IndexDeleteFailure(Long documentId, IndexTarget target, String failReason,
                              int retryCount, String status) {
        this.documentId = documentId;
        this.target = target;
        this.failReason = failReason;
        this.retryCount = retryCount;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public IndexTarget getTarget() {
        return target;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
