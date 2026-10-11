package com.kb.domain.document;

/**
 * 外部索引删除对账目标.
 *
 * @author forever-king
 */
public enum IndexTarget {

    /** Qdrant 向量索引 */
    QDRANT,

    /** Elasticsearch 全文索引 */
    ELASTICSEARCH
}
