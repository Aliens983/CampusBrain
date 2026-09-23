package com.kb.infrastructure.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * 滑动窗口限流器：Redis 正常时限流生效；Redis 故障时 fail-open 放行，
 * 不能让中间件抖动放大成全部接口不可用。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
class RateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("Lua 返回 1：放行")
    void shouldAllowWhenScriptReturnsOne() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);

        assertThat(new RateLimiter(redisTemplate).isAllowed("rl:u:1", 30, 60)).isTrue();
    }

    @Test
    @DisplayName("Lua 返回 0：限流拒绝")
    void shouldRejectWhenScriptReturnsZero() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(0L);

        assertThat(new RateLimiter(redisTemplate).isAllowed("rl:u:1", 30, 60)).isFalse();
    }

    @Test
    @DisplayName("Redis 连接故障：fail-open 放行而非整站不可用")
    void shouldFailOpenWhenRedisDown() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new RedisConnectionFailureException("connection refused"));

        assertThat(new RateLimiter(redisTemplate).isAllowed("rl:u:1", 30, 60)).isTrue();
    }

    @Test
    @DisplayName("Lua 返回 null（异常应答）：按拒绝处理，保持原语义")
    void shouldRejectWhenScriptReturnsNull() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(null);

        assertThat(new RateLimiter(redisTemplate).isAllowed("rl:u:1", 30, 60)).isFalse();
    }
}
