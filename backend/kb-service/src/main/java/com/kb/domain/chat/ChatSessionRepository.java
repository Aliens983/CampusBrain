package com.kb.domain.chat;

import java.util.Optional;

/**
 * 会话上下文仓储
 *
 * @author forever-king
 */
public interface ChatSessionRepository {

    /** 读取会话上下文（不存在返回 empty） */
    Optional<ChatSession> find(String sessionId);

    /** 读取会话上下文，不存在则按 sessionId/userId 创建 */
    ChatSession loadOrCreate(String sessionId, Long userId);

    /** 保存会话上下文 */
    void save(ChatSession session);

    /** 清空会话上下文（含槽位与待确认草稿） */
    void clear(String sessionId);
}
