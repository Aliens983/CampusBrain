package com.kb.infrastructure.rag.retrieval;

import com.kb.domain.rag.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Qualifier;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Hybrid retrieval orchestrator.
 * <p>
 * Executes keyword (ES BM25) and vector (Qdrant ANN) searches in parallel,
 * then fuses results using Reciprocal Rank Fusion (RRF).
 * </p>
 * <p>
 * RRF Formula: score = Σ 1/(k + rank_i)
 * where k is a constant (default 60), rank_i is the position in each result list.
 * This eliminates the need for manual weight tuning between keyword and vector.
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Component
public class HybridRetriever implements com.kb.domain.rag.SearchService {

    /** 关键词检索器，基于 Elasticsearch BM25 */
    private final KeywordRetriever keywordRetriever;
    /** 向量检索器，基于 Qdrant 近似最近邻搜索 */
    private final VectorRetriever vectorRetriever;
    /** Spring 管理的线程池，参与优雅关闭和 Micrometer 指标暴露 */
    private final ExecutorService executor;

    @Value("${retrieval.final-top-k}")
    /** 融合后最终返回的文档数量上限 */
    private int finalTopK;

    @Value("${retrieval.rrf-k:60}")
    /** RRF 融合算法中的常数 k，默认值 60 */
    private double rrfK;

    public HybridRetriever(KeywordRetriever keywordRetriever,
                           VectorRetriever vectorRetriever,
                           @Qualifier("retrievalExecutor") ExecutorService retrievalExecutor) {
        this.keywordRetriever = keywordRetriever;
        this.vectorRetriever = vectorRetriever;
        this.executor = retrievalExecutor;
    }

    @Override
    public List<RetrievalResult> search(String query) {
        return hybridRetrieve(query);
    }

    @Override
    public List<RetrievalResult> keywordSearch(String query) {
        return keywordRetriever.retrieve(query);
    }

    @Override
    public List<RetrievalResult> vectorSearch(String query) {
        return vectorRetriever.retrieve(query);
    }

    /**
     * Execute dual-recall + RRF fusion.
     */
    public List<RetrievalResult> hybridRetrieve(String query) {
        // 1. Parallel dual-recall
        CompletableFuture<List<RetrievalResult>> keywordFuture =
                CompletableFuture.supplyAsync(
                        () -> keywordRetriever.retrieve(query), executor);
        CompletableFuture<List<RetrievalResult>> vectorFuture =
                CompletableFuture.supplyAsync(
                        () -> vectorRetriever.retrieve(query), executor);

        List<RetrievalResult> keywordResults;
        List<RetrievalResult> vectorResults;

        try {
            keywordResults = keywordFuture.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Keyword retrieval failed", e);
            keywordResults = keywordFuture.getNow(List.of());
        }

        try {
            vectorResults = vectorFuture.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Vector retrieval failed", e);
            vectorResults = vectorFuture.getNow(List.of());
        }

        log.debug("Keyword results: {}, Vector results: {}",
                keywordResults.size(), vectorResults.size());

        // 2. RRF fusion
        List<RetrievalResult> fused = rrfFusion(keywordResults, vectorResults);

        log.debug("Fused results (after RRF): {}", fused.size());
        return fused;
    }

    /**
     * Reciprocal Rank Fusion.
     * <p>
     * For each unique chunk, sum 1/(k + rank) from each result list.
     * Chunks appearing in both lists get a higher combined score.
     * </p>
     */
    private List<RetrievalResult> rrfFusion(List<RetrievalResult> keywordResults,
                                            List<RetrievalResult> vectorResults) {
        Map<String, RetrievalResult> merged = new LinkedHashMap<>();

        // Score keyword results
        for (int i = 0; i < keywordResults.size(); i++) {
            RetrievalResult result = keywordResults.get(i);
            String key = result.getChunkId();
            double rrfScore = 1.0 / (rrfK + i + 1);
            result.setScore(rrfScore);
            merged.put(key, result);
        }

        // Score vector results — accumulate if same chunk
        for (int i = 0; i < vectorResults.size(); i++) {
            RetrievalResult result = vectorResults.get(i);
            String key = result.getChunkId();
            double rrfScore = 1.0 / (rrfK + i + 1);

            if (merged.containsKey(key)) {
                RetrievalResult existing = merged.get(key);
                existing.setScore(existing.getScore() + rrfScore);
                existing.setSource("hybrid");
            } else {
                result.setScore(rrfScore);
                merged.put(key, result);
            }
        }

        // Sort by combined RRF score descending
        List<RetrievalResult> sorted = new ArrayList<>(merged.values());
        sorted.sort(Comparator.comparingDouble(RetrievalResult::getScore).reversed());

        // Return top-K
        return sorted.subList(0, Math.min(finalTopK, sorted.size()));
    }
}
