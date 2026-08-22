package com.kb.infrastructure.rag.retrieval;

import com.kb.domain.rag.RetrievalResult;
import com.kb.infrastructure.persistence.elasticsearch.EsDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Keyword-based retrieval via Elasticsearch BM25.
 *
 * @author forever-king
 */
@Component
@RequiredArgsConstructor
public class KeywordRetriever {

    /** Elasticsearch 文档仓库，用于执行 BM25 关键词搜索 */
    private final EsDocumentRepository esDocumentRepository;

    @Value("${retrieval.top-k-keyword}")
    /** 关键词检索返回的文档数量上限 */
    private int topK;

    /**
     * Execute BM25 keyword search on document chunk content.
     */
    public List<RetrievalResult> retrieve(String query) {
        return esDocumentRepository.keywordSearch(query, topK);
    }
}
