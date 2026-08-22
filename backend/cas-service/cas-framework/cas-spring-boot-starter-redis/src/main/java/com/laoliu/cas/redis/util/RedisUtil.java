package com.laoliu.cas.redis.util;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类 - 提供通用操作方法及验证码便捷封装
 *
 * @author forever-king
 */
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ========== 通用操作 ==========

    public <T> void set(String key, T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public <T> void set(String key, T value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    public Long getExpire(String key, TimeUnit unit) {
        return redisTemplate.getExpire(key, unit);
    }

    // ========== Hash 操作 ==========

    public <T> void hSet(String key, String field, T value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T hGet(String key, String field) {
        return (T) redisTemplate.opsForHash().get(key, field);
    }

    // ========== List 操作 ==========

    public <T> void lPush(String key, T value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> lRange(String key, long start, long end) {
        return (List<T>) redisTemplate.opsForList().range(key, start, end);
    }

    // ========== 验证码便捷方法（基于通用方法） ==========

    public void setVerificationCode(String key, String value, long timeout) {
        set(key, value, timeout, TimeUnit.SECONDS);
    }

    public String getVerificationCode(String key) {
        return get(key);
    }

    public void removeVerificationCode(String key) {
        delete(key);
    }
}
