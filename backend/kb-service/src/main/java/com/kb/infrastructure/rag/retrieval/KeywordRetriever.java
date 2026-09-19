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
    public List<RetrievalResult> retrieve(String query) {
        // 4.1（深度审查 P0）：归属过滤 key —— 当前登录用户；取不到（异步线程无上下文）则仅返回共享文档（fail-closed）
        Long ownerId = SecurityFrameworkUtils.getLoginUserId();
        return esDocumentRepository.keywordSearch(query, topK, ownerId);
    }
}
