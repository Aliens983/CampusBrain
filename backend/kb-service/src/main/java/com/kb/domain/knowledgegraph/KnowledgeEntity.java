package com.kb.domain.knowledgegraph;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 知识实体 — 从文档中抽取的命名实体（人名、地名、术语、产品名等）
 *
 * @author forever-king
 */
@Getter
@Builder
public class KnowledgeEntity {

    /** 实体唯一标识 */
    private String id;

    /** 实体名称（规范形式） */
    private String name;

    /** 实体类型：PERSON / LOCATION / TERM / PRODUCT / DATE / METRIC */
    private String type;

    /** 来源文档 ID */
    private Long documentId;

    /** 来源分块 ID */
    private String chunkId;

    /** 在原文中的上下文片段 */
    private String contextSnippet;

    /** 置信度（0-1） */
    private Double confidence;

    /** 实体别名（同义词列表，JSON 数组） */
    private String aliases;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
