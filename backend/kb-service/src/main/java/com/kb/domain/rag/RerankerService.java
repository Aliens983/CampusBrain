package com.kb.domain.rag;

import java.util.List;

/**
 * Domain interface for re-ranking retrieval results.
 * <p>
 * Cross-Encoder models take both query and passage as input,
 * producing more accurate relevance scores than bi-encoder
 * vector similarity. Used as a second-stage refinement
 * after coarse retrieval.
 * </p>
 * @author forever-king
 */
public interface RerankerService {

    /**
     * Re-rank a list of candidate retrieval results.
     * <p>
     * Each candidate's score is updated to reflect the
     * Cross-Encoder relevance judgment.
     * </p>
     *
     * @param query      the user's original question
     * @param candidates the coarse-ranked candidates (typically top 10-20)
     * @return re-ranked candidates (same list, updated scores, re-sorted)
     */
    List<RetrievalResult> rerank(String query, List<RetrievalResult> candidates);
}
