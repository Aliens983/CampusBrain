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

    /**
     * 按归属用户读取会话上下文（4.1.13）：
     * <ul>
     *   <li>上下文不存在 → 以该用户身份新建内存会话；</li>
     *   <li>上下文存在且归属一致 → 返回；</li>
     *   <li>上下文存在但归属他人（sessionId 泄露/冒用）→ 返回<b>隔离的空会话</b>，
     *       调用方无法读到他人的槽位与草稿；该空会话不会覆盖 Redis 中的他人数据。</li>
     * </ul>
     */
    ChatSession loadForUser(String sessionId, Long userId);

    /** 保存会话上下文；Redis 中已存在他人同名会话时拒绝覆盖 */
    void save(ChatSession session);

    /** 清空归属用户自己的会话上下文（槽位与待确认草稿），他人上下文不受影响 */
    void clear(String sessionId, Long userId);
}
