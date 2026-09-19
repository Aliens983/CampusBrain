package com.kb.infrastructure.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Redis 滑动窗口限流器
 * <p>
 * 使用 Lua 脚本保证原子性，支持高并发场景
 * </p>
 *
 * @author forever-king
 */
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Lua 脚本：滑动窗口限流。
     * <p>
     * 4.5（深度审查 P1）修正两处：<ul>
     *   <li>TTL 原来误用 ARGV[3]=permits（如 30）当秒数，窗口 60s 时却固定 30s 过期 → key 提前消失，
     *       窗口形同虚设；现显式传入窗口秒数 ARGV[2]，EXPIRE = 窗口 + 10s 余量。</li>
     *   <li>ZADD member 原来为 {@code now-count}：同一毫秒的两请求读到相同 count 会生成相同 member，
     *       ZSET 对同 member 同 score 去重 → 并发请求总被少计；现由 Java 侧注入每请求唯一后缀。</li>
     * </ul>
     * 参数：ARGV[1]=当前毫秒时间戳；ARGV[2]=窗口秒数；ARGV[3]=窗口内允许次数；ARGV[4]=请求唯一后缀。
     */
    private static final String LUA_SCRIPT = """
        local key = KEYS[1]
        local now = tonumber(ARGV[1])
        local windowSeconds = tonumber(ARGV[2])
        local permits = tonumber(ARGV[3])
        local unique = ARGV[4]
        local windowStart = now - windowSeconds * 1000
        redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)
        local count = redis.call('ZCARD', key)
        if count < permits then
            redis.call('ZADD', key, now, now .. '-' .. unique)
            redis.call('EXPIRE', key, windowSeconds + 10)
            return 1
        else
            return 0
        end
        """;

    /**
     * 检查是否允许访问
     *
     * @param redisKey Redis Key
     * @param permits 允许次数
     * @param seconds 时间窗口（秒）
     * @return true=允许，false=限流
     */
    public boolean isAllowed(String redisKey, int permits, int seconds) {
        long now = System.currentTimeMillis();

        Long result = stringRedisTemplate.execute(
                new DefaultRedisScript<>(LUA_SCRIPT, Long.class),
                List.of(redisKey),
                String.valueOf(now),
                String.valueOf(seconds),
                String.valueOf(permits),
                UUID.randomUUID().toString()
        );
        return result != null && result == 1;
    }
}
