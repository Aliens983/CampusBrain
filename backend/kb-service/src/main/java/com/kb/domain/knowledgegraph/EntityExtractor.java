package com.kb.domain.knowledgegraph;

import java.util.List;

/**
 * 实体抽取器接口 — 从文本中提取命名实体。
 * <p>
 * 支持不同实现：LLM-based（精确但慢）、Regex-based（快速但覆盖窄）、
 * NLP pipeline（专业但重）。SPI 机制允许运行时切换。
 * </p>
 *
 * @author forever-king
 */
public interface EntityExtractor {

    /**
     * 从给定文本中抽取实体列表。
     *
     * @param text        输入文本
     * @param documentId  来源文档 ID
     * @param chunkId     来源分块 ID
     * @return 抽取出的知识实体列表
     */
    List<KnowledgeEntity> extract(String text, Long documentId, String chunkId);

    /**
     * 批量抽取（可并行优化）。
     */
    default List<KnowledgeEntity> extractBatch(List<String> texts, Long documentId,
                                                List<String> chunkIds) {
        return texts.stream()
                .flatMap(t -> extract(t, documentId,
                        chunkIds.get(texts.indexOf(t))).stream())
                .toList();
    }

    /**
     * 返回此抽取器支持的语言/领域。
     */
    default String getLanguage() { return "zh"; }
}
