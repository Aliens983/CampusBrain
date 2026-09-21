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
     * @param query  user's natural language question
     * @param userId 归属用户（null = 仅共享文档）。<b>必须显式传入</b>：检索实际执行在
     *               线程池上，SecurityContext 是 MODE_THREADLOCAL，池线程取不到身份
     *               （P1-01）。由调用方在请求线程解析一次后沿链路传下来。
     * @return fused and ranked retrieval results (up to configured top-K)
     */
    List<RetrievalResult> search(String query, Long userId);

    /**
     * Execute keyword-only search (for debugging/comparison).
     */
    List<RetrievalResult> keywordSearch(String query, Long userId);

    /**
     * Execute vector-only search (for debugging/comparison).
     */
    List<RetrievalResult> vectorSearch(String query, Long userId);
}
