package com.kb.domain.rag;

import java.util.List;
import java.util.Map;

/**
 * Domain interface for vector storage and similarity search.
 * <p>
 * Currently backed by Qdrant. Abstracts the vector DB operations
 * so the domain layer doesn't depend on Qdrant specifics.
 * </p>
 * @author forever-king
 */
public interface VectorStoreService {

    /**
     * Ensure the target collection exists (idempotent).
     * Called at application startup.
     */
    void ensureCollection();

    /**
     * Batch-upsert vectors with payload into the store.
     *
     * @param points list of (id, vector, payload) tuples
     */
    void upsert(List<VectorPoint> points);

    /**
     * Search for the most similar vectors to the query vector.
     *
     * @param queryVector    the query embedding
     * @param limit          max number of results
     * @param scoreThreshold minimum similarity score (0.0 - 1.0)
     * @return scored results with full payload
     */
    List<ScoredVector> search(float[] queryVector, int limit, double scoreThreshold);

    /**
     * Delete vectors by a list of point IDs.
     */
    void delete(List<String> pointIds);

    /**
     * Delete all vectors associated with a document.
     */
    void deleteByDocumentId(String documentId);

    // ========== Inner Types ==========

    /**
     * A vector point to upsert.
     */
    record VectorPoint(String id, float[] vector, Map<String, Object> payload) {}

    /**
     * A search result with score and payload.
     */
    record ScoredVector(String id, double score, Map<String, Object> payload) {}
}
