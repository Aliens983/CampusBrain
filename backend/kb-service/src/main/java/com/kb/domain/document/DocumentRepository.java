package com.kb.domain.document;

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
     * Find all documents, ordered by creation time descending.
     * Use {@link #findAll(int, int)} for paginated queries.
     */
    List<Document> findAll();

    /**
     * Find documents with pagination.
     * @param page 0-based page number
     * @param size page size
     */
    List<Document> findAll(int page, int size);

    /**
     * Find documents by processing status.
     */
    List<Document> findByStatus(DocumentStatus status);

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
     * 删除某个文档的全部分块（用于重试时清空旧数据，保证幂等）。
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
