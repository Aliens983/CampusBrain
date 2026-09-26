package com.kb.domain.conversation;

import java.time.LocalDateTime;

/**
 * 会话摘要（历史会话列表中的一条）。
 * <p>
 * 会话本身没有独立的表，元数据由 conversation 消息行聚合得到：
 * title 取该会话第一条用户消息，updatedAt 取最后一条消息时间。
 * </p>
 *
 * @param sessionId 会话 ID
 * @param title     会话标题（首条用户提问原文）
 * @param updatedAt 最后一条消息时间
 * @author forever-king
 */
public record ConversationSummary(String sessionId, String title, LocalDateTime updatedAt) {
}
