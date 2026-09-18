package com.kb.domain.document;

/**
 * 问答缓存联动淘汰南向端口（A-01 应用层端口化；12-04 文档删除/重处理联动）。
 * <p>
 * 文档内容发生变化（删除/重处理）后，问答缓存中可能残留引用旧内容的答案，
 * 应用层通过本端口触发两级问答缓存的整体淘汰。
 * </p>
 *
 * @author forever-king
 */
public interface KnowledgeCacheInvalidator {

    /**
     * 按变更原因整体淘汰两级问答缓存（精确 + 语义）。
     *
     * @param reason 变更原因（用于日志定位，如 "文档删除 documentId=3"）
     */
    void evictAllQaCaches(String reason);
}