package com.kb.domain.document;

/**
 * 文档索引删除南向端口（A-01 应用层端口化）。
 * <p>
 * 声明"删除某个文档的全部外部全文索引"这一领域能力，由基础设施层实现
 * （当前为 Elasticsearch），应用层只依赖本端口，不再直接面向具体索引实现。
 * </p>
 *
 * @author forever-king
 */
public interface DocumentIndexCleaner {

    /**
     * 删除指定文档在全文索引（ES）中的所有分块。
     *
     * @param documentId 文档 ID（字符串形式）
     */
    void deleteByDocumentId(String documentId);
}