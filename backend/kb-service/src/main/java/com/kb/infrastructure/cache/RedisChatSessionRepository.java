package com.kb.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.domain.chat.ChatSession;
import com.kb.domain.chat.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 会话上下文的 Redis 实现
 * <p>
 * 用 Redis 而非 MySQL 的原因：上下文是高频读写的临时状态，
 * 且自带 TTL 可自然过期，无需清理任务。
 * 消息正文仍落 MySQL（conversation 表）供历史回看。
 * </p>
 * <p>
 * 4.15（深度审查 P2）：save 由"GET 归属校验 → SET"两步改为<b>Lua 原子</b>
 * （归属校验与写入同一条脚本执行，消除 TOCTOU，也省一次网络往返）；
 * 归属以独立 owner key（{@code OWNER_KEY_PREFIX}）记录并与主体 key 同 TTL，
 * 无主会话（owner 为空串）仅允许覆盖同为无主的数据，语义与原实现完全一致。
 * 注：升级前遗留的无 owner key 主体 key 由首次 save 依据自身 owner 补建。
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisChatSessionRepository implements ChatSessionRepository {

    private static final String KEY_PREFIX = "kb:chat:session:";
    /** 4.15：独立记录会话归属的 key 前缀（供 save 的 Lua 原子归属校验） */
    private static final String OWNER_KEY_PREFIX = "kb:chat:session:owner:";

    /** 会话上下文保留时长 */
    private static final Duration TTL = Duration.ofHours(6);

    /**
     * 4.15：归属校验 + 写入的原子 Lua。
     * <ul>
     *   <li>owner key 已存在且与本次 owner 不同 → 拒绝（return 0）；</li>
     *   <li>首次写入（owner key 不存在）→ 直接写入并登记 owner（与主体同 TTL）；</li>
     *   <li>owner 为空串表示无主：仅允许覆盖同为无主（owner 也为空串）的会话。</li>
     * </ul>
     */
    private static final String SAVE_LUA = """
        local mainKey = KEYS[1]
        local ownerKey = KEYS[2]
        local owner = ARGV[1]
        local payload = ARGV[2]
        local ttl = tonumber(ARGV[3])
        if redis.call('EXISTS', ownerKey) == 1 then
            if redis.call('GET', ownerKey) ~= owner then
                return 0
            end
        end
        redis.call('SET', mainKey, payload, 'EX', ttl)
        redis.call('SET', ownerKey, owner, 'EX', ttl)
        return 1
        """;

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
        session.setUpdatedAt(System.currentTimeMillis());
        String json;
        try {
            json = objectMapper.writeValueAsString(session);
        } catch (JsonProcessingException e) {
            log.warn("会话上下文序列化失败: sessionId={}", session.getSessionId(), e);
            return;
        }
        try {
            // 4.15：归属校验 + 写入收敛为一条 Lua（原子，消除 TOCTOU）
            Object result = redisTemplate.execute(
                    new DefaultRedisScript<>(SAVE_LUA, Long.class),
                    List.of(KEY_PREFIX + session.getSessionId(),
                            OWNER_KEY_PREFIX + session.getSessionId()),
                    session.getUserId() == null ? "" : String.valueOf(session.getUserId()),
                    json,
                    String.valueOf(TTL.getSeconds()));
            if (result instanceof Long l && l == 0L) {
                log.warn("拒绝写入他人会话上下文: sessionId={}, owner={}",
                        session.getSessionId(), session.getUserId());
            }
        } catch (Exception e) {
            // Redis 故障/脚本失败：本次不持久化，不能影响问答主流程
            log.warn("写入会话上下文失败（本次不持久化）: sessionId={}", session.getSessionId(), e);
        }
    }

    @Override
    public void clear(String sessionId, Long userId) {
        if (sessionId == null) {
            return;
        }
        // 仅允许清空归属自己的上下文；key 不存在时 delete 天然幂等。
        // userId 为 null 时必须拒绝（而不是放行）：取不到身份说明调用链上没有登录态，
        // 放行等于任何人都能清空任意会话。此处与 MySQL 侧（SQL 带 AND user_id = ?，
        // userId 为 null 时一行都不命中）保持一致的 fail-closed 语义。
        ChatSession existing = find(sessionId).orElse(null);
        if (existing != null && (userId == null || !userId.equals(existing.getUserId()))) {
            log.warn("拒绝清空他人会话上下文: sessionId={}, owner={}, currentUser={}",
                    sessionId, existing.getUserId(), userId);
            return;
        }
        try {
            redisTemplate.delete(List.of(KEY_PREFIX + sessionId,
                    OWNER_KEY_PREFIX + sessionId));
        } catch (Exception e) {
            log.warn("清空会话上下文失败: sessionId={}", sessionId, e);
        }
    }
}
