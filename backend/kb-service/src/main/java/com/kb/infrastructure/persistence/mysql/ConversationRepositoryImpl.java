package com.kb.infrastructure.persistence.mysql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.domain.conversation.Conversation;
import com.kb.domain.conversation.ConversationRepository;
import com.kb.infrastructure.persistence.mysql.dataobject.ConversationDO;
import com.kb.infrastructure.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

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
    public void save(String sessionId, String role, String content) {
        saveWithReferences(sessionId, role, content, null);
    }

    @Override
    public Long saveWithReferences(String sessionId, String role, String content,
                                    List<Conversation.CitationRef> references) {
        ConversationDO convDO = new ConversationDO();
        convDO.setSessionId(sessionId);
        convDO.setRole(role);
        convDO.setContent(content);
        convDO.setReferencesJson(toJson(references));
        convDO.setTenantId(TenantContext.getTenantId());
        conversationMapper.insert(convDO);
        // MyBatis-Plus 自增主键回填
        return convDO.getId();
    }

    @Override
    public List<Conversation> getRecentMessages(String sessionId, int limit) {
        return conversationMapper.selectRecentBySessionId(sessionId, limit).stream()
                .map(this::toConversation)
                .toList();
    }

    @Override
    public List<Conversation> findBySessionId(String sessionId) {
        return conversationMapper.selectBySessionId(sessionId).stream()
                .map(this::toConversation)
                .toList();
    }

    @Override
    public void updateFeedback(Long messageId, String feedback) {
        conversationMapper.updateFeedback(messageId, feedback);
    }

    @Override
    public Optional<Conversation> findById(Long id) {
        ConversationDO convDO = conversationMapper.selectById(id);
        return Optional.ofNullable(convDO).map(this::toConversation);
    }

    @Override
    public void deleteBySessionId(String sessionId) {
        conversationMapper.deleteBySessionId(sessionId);
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
                .role(convDO.getRole())
                .content(convDO.getContent())
                .references(parseReferences(convDO.getReferencesJson()))
                .feedback(convDO.getFeedback())
                .tenantId(convDO.getTenantId())
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
