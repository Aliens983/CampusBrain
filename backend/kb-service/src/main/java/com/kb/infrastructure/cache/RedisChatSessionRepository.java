package com.kb.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.domain.chat.ChatSession;
import com.kb.domain.chat.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * 会话上下文的 Redis 实现
 * <p>
 * 用 Redis 而非 MySQL 的原因：上下文是高频读写的临时状态，
 * 且自带 TTL 可自然过期，无需清理任务。
 * 消息正文仍落 MySQL（conversation 表）供历史回看。
 *
 * @author forever-king
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisChatSessionRepository implements ChatSessionRepository {

    private static final String KEY_PREFIX = "kb:chat:session:";

    /** 会话上下文保留时长 */
    private static final Duration TTL = Duration.ofHours(6);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<ChatSession> find(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        String json;
        try {
            json = redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);
        } catch (Exception e) {
            // Redis 不可用时降级为"无上下文"，不能因此打断问答主流程
            log.warn("读取会话上下文失败，按空会话处理: sessionId={}", sessionId, e);
            return Optional.empty();
        }
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(objectMapper.readValue(json, ChatSession.class));
        } catch (JsonProcessingException e) {
            log.warn("会话上下文反序列化失败，按空会话处理: sessionId={}", sessionId);
            return Optional.empty();
        }
    }

    @Override
    public ChatSession loadForUser(String sessionId, Long userId) {
        ChatSession existing = find(sessionId).orElse(null);
        if (existing == null) {
            return ChatSession.create(sessionId, userId);
        }
        if (userId != null && userId.equals(existing.getUserId())) {
            return existing;
        }
        // sessionId 与上下文归属不一致（泄露/冒用）：返回隔离空会话，
        // 既不暴露他人槽位/草稿，也不允许后续 save 覆盖他人上下文
        log.warn("检测到会话上下文归属不一致，返回隔离空会话: sessionId={}, owner={}, currentUser={}",
                sessionId, existing.getUserId(), userId);
        return ChatSession.create(sessionId, userId);
    }

    @Override
    public void save(ChatSession session) {
        if (session == null || session.getSessionId() == null) {
            return;
        }
        // 防御：Redis 中已存在他人同名会话时绝不覆盖（4.1.13）
        ChatSession existing = find(session.getSessionId()).orElse(null);
        if (existing != null && session.getUserId() != null
                && !session.getUserId().equals(existing.getUserId())) {
            log.warn("拒绝写入他人会话上下文: sessionId={}, owner={}, currentUser={}",
                    session.getSessionId(), existing.getUserId(), session.getUserId());
            return;
        }
        session.setUpdatedAt(System.currentTimeMillis());
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + session.getSessionId(),
                    objectMapper.writeValueAsString(session),
                    TTL);
        } catch (JsonProcessingException e) {
            log.warn("会话上下文序列化失败: sessionId={}", session.getSessionId(), e);
        } catch (Exception e) {
            log.warn("写入会话上下文失败（本次不持久化）: sessionId={}", session.getSessionId(), e);
        }
    }

    @Override
    public void clear(String sessionId, Long userId) {
        if (sessionId == null) {
            return;
        }
        // 仅允许清空归属自己的上下文；key 不存在时 delete 天然幂等
        ChatSession existing = find(sessionId).orElse(null);
        if (existing != null && userId != null && !userId.equals(existing.getUserId())) {
            log.warn("拒绝清空他人会话上下文: sessionId={}, owner={}, currentUser={}",
                    sessionId, existing.getUserId(), userId);
            return;
        }
        try {
            redisTemplate.delete(KEY_PREFIX + sessionId);
        } catch (Exception e) {
            log.warn("清空会话上下文失败: sessionId={}", sessionId, e);
        }
    }
}
