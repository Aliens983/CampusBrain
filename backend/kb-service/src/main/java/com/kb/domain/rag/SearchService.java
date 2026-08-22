package com.kb.domain.rag;

import java.util.List;

/**
 * Domain interface for the hybrid search pipeline.
 * <p>
 * Orchestrates multi-recall (keyword + vector) and RRF fusion
 * to produce a final ranked list of relevant document chunks.
 * </p>
 * @author forever-king
 */
public interface SearchService {

    /**
     * Execute hybrid search: keyword (ES) + vector (Qdrant) → RRF fusion.
     *
     * @param query user's natural language question
     * @return fused and ranked retrieval results (up to configured top-K)
     */
    List<RetrievalResult> search(String query);

    /**
     * Execute keyword-only search (for debugging/comparison).
     */
    List<RetrievalResult> keywordSearch(String query);

    /**
     * Execute vector-only search (for debugging/comparison).
     */
    List<RetrievalResult> vectorSearch(String query);
}
