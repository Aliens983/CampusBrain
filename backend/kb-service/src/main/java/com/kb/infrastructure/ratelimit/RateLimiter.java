package com.kb.infrastructure.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis 滑动窗口限流器
 * <p>
 * 使用 Lua 脚本保证原子性，支持高并发场景。
 * </p>
 *
 * @author forever-king
 */
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final StringRedisTemplate stringRedisTemplate;

    /** Lua 脚本：滑动窗口限流 */
    private static final String LUA_SCRIPT = """
        local key = KEYS[1]
        local now = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local permits = tonumber(ARGV[3])
        redis.call('ZREMRANGEBYSCORE', key, 0, window)
        local count = redis.call('ZCARD', key)
        if count < permits then
            redis.call('ZADD', key, now, now .. '-' .. count)
            redis.call('EXPIRE', key, tonumber(ARGV[3]) + 10)
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
        long windowStart = now - seconds * 1000L;

        Long result = stringRedisTemplate.execute(
                new DefaultRedisScript<>(LUA_SCRIPT, Long.class),
                List.of(redisKey),
                String.valueOf(now),
                String.valueOf(windowStart),
                String.valueOf(permits)
        );
        return result != null && result == 1;
    }
}
