package com.kb.domain.conversation;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Conversation entity.
 * @author forever-king
 */
public interface ConversationRepository {

    /**
     * Save a conversation message.
     */
    void save(String sessionId, String role, String content);

    /**
     * Save a conversation message with citation references.
     *
     * @return 保存后的消息 ID（供前端反馈使用）
     */
    Long saveWithReferences(String sessionId, String role, String content,
                            List<Conversation.CitationRef> references);

    /**
     * Get the most recent N messages for a session (chronological order).
     */
    List<Conversation> getRecentMessages(String sessionId, int limit);

    /**
     * Find all messages in a session.
     */
    List<Conversation> findBySessionId(String sessionId);

    /**
     * Update feedback for a specific message.
     */
    void updateFeedback(Long messageId, String feedback);

    /**
     * Find a message by ID.
     */
    Optional<Conversation> findById(Long id);

    /**
     * Delete all messages in a session.
     */
    void deleteBySessionId(String sessionId);

    /**
     * Count total conversation messages.
     */
    long count();
}
