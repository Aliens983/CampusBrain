package com.kb.infrastructure.persistence.mysql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.domain.conversation.Conversation;
import com.kb.domain.conversation.ConversationRepository;
import com.kb.infrastructure.common.BusinessException;
import com.kb.infrastructure.common.ErrorCode;
import com.kb.infrastructure.persistence.mysql.dataobject.ConversationDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ConversationRepository implementation backed by MySQL.
 *
 * @author forever-king
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ConversationRepositoryImpl implements ConversationRepository {

    /** 会话Mapper */
    private final ConversationMapper conversationMapper;

    /** JSON序列化/反序列化工具 */
    private final ObjectMapper objectMapper;

    @Override
    public Long save(String sessionId, String role, String content, Long userId) {
        return saveWithReferences(sessionId, role, content, null, userId);
    }

    @Override
    @Transactional
    public Long saveWithReferences(String sessionId, String role, String content,
                                    List<Conversation.CitationRef> references, Long userId) {
        ensureSessionOwnership(sessionId, userId);
        ConversationDO convDO = new ConversationDO();
        convDO.setSessionId(sessionId);
        convDO.setUserId(userId);
        convDO.setRole(role);
        convDO.setContent(content);
        convDO.setReferencesJson(toJson(references));
        conversationMapper.insert(convDO);
        // MyBatis-Plus 自增主键回填
        return convDO.getId();
    }

    /**
     * 会话归属闸门（4.1.13）：
     * <ul>
     *   <li>会话已有 owner 且不是当前用户 → 拒绝写入（sessionId 泄露不能冒用他人会话）；</li>
     *   <li>会话存在但消息均无主（V2 前的存量数据）→ 首写者认领；</li>
     *   <li>会话无任何消息 → 本次写入自然完成绑定。</li>
     * </ul>
     */
    private void ensureSessionOwnership(String sessionId, Long userId) {
        Long owner = conversationMapper.selectOwnerUserIdBySessionId(sessionId);
        if (owner == null) {
            // 无消息的新会话由本次 insert 自然绑定；
            // 仅有无主存量消息时，首写者一次性认领（无行则影响 0 行）
            conversationMapper.bindOwnerlessMessages(sessionId, userId);
            return;
        }
        if (owner.equals(userId)) {
            return;
        }
        log.warn("拒绝跨用户写入会话: sessionId={}, owner={}, currentUser={}",
                sessionId, owner, userId);
        throw new BusinessException.QaException(ErrorCode.QA_SESSION_FORBIDDEN,
                "sessionId=" + sessionId);
    }

    @Override
    public List<Conversation> getRecentMessages(String sessionId, int limit, Long userId) {
        return conversationMapper.selectRecentBySessionId(sessionId, userId, limit).stream()
                .map(this::toConversation)
                .toList();
    }

    @Override
    public List<Conversation> findBySession(String sessionId, Long userId) {
        return conversationMapper.selectBySessionId(sessionId, userId).stream()
                .map(this::toConversation)
                .toList();
    }

    @Override
    public boolean updateFeedback(Long messageId, String feedback, Long userId) {
        return conversationMapper.updateFeedback(messageId, feedback, userId) > 0;
    }

    @Override
    public Optional<Conversation> findById(Long id) {
        ConversationDO convDO = conversationMapper.selectById(id);
        return Optional.ofNullable(convDO).map(this::toConversation);
    }

    @Override
    public Long findOwnerIdBySessionId(String sessionId) {
        return conversationMapper.selectOwnerUserIdBySessionId(sessionId);
    }

    @Override
    public void deleteBySession(String sessionId, Long userId) {
        conversationMapper.deleteBySessionId(sessionId, userId);
    }

    @Override
    public long count() {
        return conversationMapper.selectCount(null);
    }

    // ========== Conversion ==========

    private Conversation toConversation(ConversationDO convDO) {
        return Conversation.builder()
                .id(convDO.getId())
                .sessionId(convDO.getSessionId())
                .userId(convDO.getUserId())
                .role(convDO.getRole())
                .content(convDO.getContent())
                .references(parseReferences(convDO.getReferencesJson()))
                .feedback(convDO.getFeedback())
                .createdAt(convDO.getCreatedAt())
                .build();
    }

    private List<Conversation.CitationRef> parseReferences(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse references JSON", e);
            return null;
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize to JSON", e);
            return null;
        }
    }
}
