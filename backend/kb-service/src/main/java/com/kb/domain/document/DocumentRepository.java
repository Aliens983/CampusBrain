package com.kb.domain.document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Document aggregate root.
 * Implemented in infrastructure layer with MyBatis-Plus.
 * @author forever-king
 */
public interface DocumentRepository {

    /**
     * Save or update a document.
     */
    Document save(Document document);

    /**
     * Find a document by ID.
     */
    Optional<Document> findById(Long id);

    /**
     * Find documents with pagination, ordered by creation time descending.
     * @param page 0-based page number
     * @param size page size
     */
    List<Document> findAll(int page, int size);

    /**
     * Find documents by processing status.
     */
    List<Document> findByStatus(DocumentStatus status);

    /**
     * 查找长时间停留在给定中间态、updated_at 早于阈值的文档（超时回收用，12-03）。
     *
     * @param statuses  中间态集合（UPLOADED/PARSING/CHUNKING/EMBEDDING）
     * @param threshold updated_at 上界（早于该时间才算卡死）
     * @param limit     单次最多返回条数
     */
    List<Document> findStuckInProcessing(List<DocumentStatus> statuses,
                                         LocalDateTime threshold, int limit);

    /**
     * 按归属用户分页查询，按创建时间倒序。
     *
     * @param ownerId 归属用户ID
     * @param page    0 基页码
     * @param size    每页条数
     */
    List<Document> findByOwnerId(Long ownerId, int page, int size);

    /**
     * 按归属用户 + 标题关键词分页查询（下推 SQL，避免全表内存过滤）。
     *
     * @param ownerId 归属用户ID
     * @param keyword 标题关键词
     * @param page    0 基页码
     * @param size    每页条数
     */
    List<Document> searchByOwnerIdAndTitle(Long ownerId, String keyword, int page, int size);

    /**
     * 管理员视角：全量标题关键词分页查询（无 owner 条件，下推 SQL）。
     *
     * @param keyword 标题关键词
     * @param page    0 基页码
     * @param size    每页条数
     */
    List<Document> searchAllByTitle(String keyword, int page, int size);

    /**
     * Update document status only.
     */
    void updateStatus(Long id, DocumentStatus status, String errorMsg);

    /**
     * Update chunk count and set status to READY.
     */
    void markReady(Long id, int chunkCount);

    /**
     * Save chunk records in batch.
     */
    void saveChunks(List<DocumentChunk> chunks, Long documentId);

    /**
     * Find chunks by document ID.
     */
    List<DocumentChunk> findChunksByDocumentId(Long documentId);

    /**
     * Delete a document and its chunks (cascade).
     */
    void delete(Long id);

    /**
     * 删除某个文档的全部分块（用于重试时清空旧数据，保证幂等）
     */
    void deleteChunksByDocumentId(Long documentId);

    /**
     * Count total documents.
     */
    long count();

    /**
     * Count documents by status.
     */
    long countByStatus(DocumentStatus status);
}
