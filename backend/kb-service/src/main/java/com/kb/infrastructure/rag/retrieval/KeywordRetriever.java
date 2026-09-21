package com.kb.infrastructure.rag.retrieval;

import com.kb.domain.rag.RetrievalResult;
import com.kb.infrastructure.persistence.elasticsearch.EsDocumentRepository;
import com.kb.infrastructure.security.SecurityFrameworkUtils;
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
    /**
     * @param query   检索词
     * @param ownerId 归属用户（null = 仅共享文档，fail-closed）
     *                <p>
     *                P1-01：原先在方法内取 {@code SecurityFrameworkUtils.getLoginUserId()}，
     *                但混合检索是在 {@code retrievalExecutor} 池线程上执行的，而 SecurityContext
     *                是 MODE_THREADLOCAL（池线程从未 set 过身份）→ ownerId 恒为 null →
     *                <b>用户私有文档永远无法被召回</b>。改为由调用方在请求线程解析后显式传入。
     */
    public List<RetrievalResult> retrieve(String query, Long ownerId) {
        return esDocumentRepository.keywordSearch(query, topK, ownerId);
    }
}
