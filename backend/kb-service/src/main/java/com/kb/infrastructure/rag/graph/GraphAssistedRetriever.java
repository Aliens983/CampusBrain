package com.kb.infrastructure.rag.graph;

import com.kb.domain.rag.RetrievalResult;
import com.kb.domain.rag.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 图谱辅助检索器 — 在标准混合检索前进行查询扩展。
 * <p>
 * 流程：
 * <ol>
 *   <li>原始 query → 知识图谱实体扩展</li>
 *   <li>扩展后的 keywords 附加到原始 query</li>
 *   <li>委托给标准 {@link SearchService} 执行混合检索</li>
 * </ol>
 * 这解决了「用户用口语化提问，但文档使用专业术语」的语义鸿沟问题。
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GraphAssistedRetriever {

    private final SearchService searchService;
    private final KnowledgeGraphService kgService;

    /**
     * 图谱增强检索。
     *
     * @param query 用户原始查询
     * @return 融合后的检索结果
     */
    public List<RetrievalResult> retrieve(String query) {
        // Step 1: 实体扩展
        Set<String> expansions = kgService.expandQuery(query);

        // Step 2: 构建增强查询（原始 query + 扩展实体作为关键词）
        String enhancedQuery = query;
        if (!expansions.isEmpty()) {
            String keywords = String.join(" ", expansions);
            enhancedQuery = query + " " + keywords;
            log.debug("Graph-assisted query: [{}] → [{}]", query, enhancedQuery);
        }

        // Step 3: 委托标准混合检索
        return searchService.search(enhancedQuery);
    }

}
