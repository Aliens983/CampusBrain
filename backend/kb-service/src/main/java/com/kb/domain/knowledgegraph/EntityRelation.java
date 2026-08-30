package com.kb.domain.knowledgegraph;

import lombok.Builder;
import lombok.Getter;

/**
 * 实体关系 — 两个知识实体之间的关联边
 *
 * @author forever-king
 */
@Getter
@Builder
public class EntityRelation {

    /** 关系唯一标识 */
    private String id;

    /** 源实体 ID */
    private String sourceEntityId;

    /** 目标实体 ID */
    private String targetEntityId;

    /** 关系类型：RELATED_TO / PART_OF / LOCATED_IN / SAME_AS / REFERENCES */
    private String relationType;

    /** 共现权重（在同一分块中出现的次数） */
    private Integer cooccurrenceCount;

    /** 来源文档 ID */
    private Long documentId;

    /** 关系置信度 */
    private Double confidence;
}
