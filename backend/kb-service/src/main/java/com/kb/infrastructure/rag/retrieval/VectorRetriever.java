package com.kb.infrastructure.rag.retrieval;

import com.kb.domain.rag.EmbeddingService;
import com.kb.domain.rag.RetrievalResult;
import com.kb.domain.rag.VectorStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Vector-based semantic retrieval via Qdrant approximate nearest neighbor search.
 *
 * @author forever-king
 */
@Component
@RequiredArgsConstructor
public class VectorRetriever {

    /** 向量存储服务，用于执行近似最近邻搜索 */
    private final VectorStoreService vectorStore;
    /** 嵌入服务，用于将查询文本转换为向量 */
    private final EmbeddingService embeddingService;

    @Value("${retrieval.top-k-vector}")
    /** 向量检索返回的文档数量上限 */
    private int topK;

    @Value("${retrieval.similarity-threshold}")
    /** 相似度阈值，低于此分数的结果将被过滤 */
    private double similarityThreshold;

    /**
     * Convert query to embedding and search Qdrant for similar vectors.
     */
    public List<RetrievalResult> retrieve(String query) {
        // 1. Embed the query
        float[] queryVector = embeddingService.embed(query);

        // 2. Search Qdrant
        List<VectorStoreService.ScoredVector> results =
                vectorStore.search(queryVector, topK, similarityThreshold);

        // 3. Map to domain results
        return results.stream()
                .map(sv -> RetrievalResult.builder()
                        .chunkId(sv.payload() != null
                                ? (String) sv.payload().getOrDefault("chunk_id", sv.id())
                                : sv.id())
                        .documentId(sv.payload() != null
                                ? (String) sv.payload().getOrDefault("document_id", "")
                                : "")
                        .documentTitle(sv.payload() != null
                                ? (String) sv.payload().getOrDefault("document_title", "")
                                : "")
                        .content(sv.payload() != null
                                ? (String) sv.payload().getOrDefault("content", "")
                                : "")
                        .chunkIndex(sv.payload() != null
                                && sv.payload().get("chunk_index") != null
                                ? ((Number) sv.payload().get("chunk_index")).intValue()
                                : 0)
                        .score(sv.score())
                        .source("vector")
                        .build())
                .toList();
    }
}
