/**
 * 文档写路径的领域端口（2.6 深度审查 P2 判据）。
 * <p>
 * <b>端口约定</b>：领域端口仅用于<b>文档写路径</b>——投递（{@code DocumentProcessingDispatcher}）、
 * 外部索引清理（{@code DocumentIndexCleaner}）、问答缓存联动失效（{@code KnowledgeCacheInvalidator}）。
 * 这些操作可以被替换实现（MQ / ES / 内存清缓存），因此暴露为南向端口由基础设施实现。
 * </p>
 * <p>
 * 问答<b>读路径</b>（检索、缓存读取、回答生成）与运行时支撑（指标、Redis 计数器等）
 * 在 kb-service 内直接依赖具体基础设施实现，不为其重复造端口——避免"什么时候走端口"
 * 无判据导致端口数量膨胀。
 * </p>
 */
package com.kb.domain.document;