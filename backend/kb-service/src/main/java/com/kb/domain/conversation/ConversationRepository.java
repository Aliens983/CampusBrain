package com.kb.domain.conversation;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Conversation entity.
 *
 * @author forever-king
 */
public interface ConversationRepository {

    /**
     * 保存一条会话消息。会话在首条消息写入时绑定归属用户，
     * 后续写入必须为同一用户，否则抛出业务异常（4.1.13）。
     */
    Long save(String sessionId, String role, String content, Long userId);

    /**
     * Save a conversation message with citation references.
     *
     * @return 保存后的消息 ID（供前端反馈使用）
     */
    Long saveWithReferences(String sessionId, String role, String content,
                            List<Conversation.CitationRef> references, Long userId);

    /**
     * Get the most recent N messages for a session (chronological order).
     * 仅返回归属 userId 的消息。
     */
    List<Conversation> getRecentMessages(String sessionId, int limit, Long userId);

    /**
     * Find all messages in a session for the given owner.
     */
    List<Conversation> findBySession(String sessionId, Long userId);

    /**
     * 更新消息反馈，带归属条件。
     *
     * @return true 表示命中且更新成功；false 表示消息不存在或不属于该用户
     */
    boolean updateFeedback(Long messageId, String feedback, Long userId);

    /**
     * Find a message by ID.
     */
    Optional<Conversation> findById(Long id);

    /**
     * 查询会话归属用户 ID；会话无消息或存量数据无主时返回 null。
     */
    Long findOwnerIdBySessionId(String sessionId);

    /**
     * Delete all messages owned by userId in a session.
     */
    void deleteBySession(String sessionId, Long userId);

    /**
     * Count total conversation messages.
     */
    long count();
}
