package com.kb.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话缓存服务
 * <p>
 * 多轮对话上下文存 Redis，减少 MySQL 查询压力：
 * <ul>
 *   <li>每会话最多保留 20 条消息</li>
 *   <li>TTL 2 小时，到期自动清理</li>
 *   <li>MySQL 存全量，Redis 只做热缓存</li>
 * </ul>
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final StringRedisTemplate stringRedisTemplate;
    private final CacheKeyBuilder keyBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /** 每会话最多保留消息数 */
    private static final int MAX_MESSAGES = 20;

    /** 会话 TTL */
    private static final Duration SESSION_TTL = Duration.ofHours(2);

    /**
     * 追加一条消息到会话
     */
    public void appendMessage(String sessionId, String role, String content) {
        try {
            String key = keyBuilder.sessionKey(sessionId);
            SessionMessage msg = new SessionMessage(role, content,
                    System.currentTimeMillis());
            stringRedisTemplate.opsForList().rightPush(
                    key, objectMapper.writeValueAsString(msg));
            Long size = stringRedisTemplate.opsForList().size(key);
            if (size != null && size > MAX_MESSAGES) {
                stringRedisTemplate.opsForList().trim(key, -MAX_MESSAGES, -1);
            }
            stringRedisTemplate.expire(key, SESSION_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize session message", e);
        }
    }

    /**
     * 获取最近 N 条消息
     */
    public List<SessionMessage> getRecentMessages(String sessionId, int count) {
        String key = keyBuilder.sessionKey(sessionId);
        List<String> jsons = stringRedisTemplate.opsForList()
                .range(key, -count, -1);
        if (jsons == null || jsons.isEmpty()) {
            return List.of();
        }
        List<SessionMessage> messages = new ArrayList<>();
        for (String json : jsons) {
            try {
                messages.add(objectMapper.readValue(json, SessionMessage.class));
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize session message", e);
            }
        }
        return messages;
    }

    /**
     * 会话消息记录
     */
    public record SessionMessage(String role, String content, long timestamp) {}
}
