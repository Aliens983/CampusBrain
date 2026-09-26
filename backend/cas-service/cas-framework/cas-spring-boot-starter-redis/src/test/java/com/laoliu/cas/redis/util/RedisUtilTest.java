package com.laoliu.cas.redis.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * RedisUtil 计数器读取回归测试。
 * <p>
 * 背景：登录失败计数由 Redis INCR 写入裸数字串，再经 Jackson2Json 反序列化，
 * 小数值会还原成 Integer。旧代码用泛型 {@code redisUtil.<Long>get(key)} 接收，
 * checkcast Long 直接抛 ClassCastException: Integer cannot be cast to Long。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisUtil 计数器读取")
class RedisUtilTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Test
    @DisplayName("INCR 计数被反序列化为 Integer 时，getCounter 正常转成 Long（ClassCastException 回归）")
    void shouldReturnLongWhenCounterDeserializedAsInteger() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("login:fail:user@example.com")).thenReturn(1);

        RedisUtil redisUtil = new RedisUtil(redisTemplate);

        Long count = redisUtil.getCounter("login:fail:user@example.com");
        assertEquals(1L, count);
    }

    @Test
    @DisplayName("大数值反序列化为 Long 时原样返回")
    void shouldReturnLongWhenCounterDeserializedAsLong() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("counter")).thenReturn(123456789012L);

        RedisUtil redisUtil = new RedisUtil(redisTemplate);

        assertEquals(123456789012L, redisUtil.getCounter("counter"));
    }

    @Test
    @DisplayName("key 不存在时返回 null")
    void shouldReturnNullWhenCounterAbsent() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("counter")).thenReturn(null);

        RedisUtil redisUtil = new RedisUtil(redisTemplate);

        assertNull(redisUtil.getCounter("counter"));
    }
}
