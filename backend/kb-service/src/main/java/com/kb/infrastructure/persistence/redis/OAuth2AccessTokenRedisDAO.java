package com.kb.infrastructure.persistence.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kb.infrastructure.persistence.mysql.dataobject.OAuth2AccessTokenDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Access Token Redis 缓存 DAO
 * <p>
 * Key 格式: oauth2_access_token:{token值}
 * TTL = 距离过期时间的秒数，过期自动删除
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class OAuth2AccessTokenRedisDAO {

    private static final String KEY_PREFIX = "oauth2_access_token:";
    private static final String REFRESH_TOKEN_INDEX_PREFIX = "oauth2_at_by_rt:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * 缓存 Access Token
     */
    public void set(OAuth2AccessTokenDO token) {
        try {
            String key = KEY_PREFIX + token.getAccessToken();
            String json = objectMapper.writeValueAsString(token);
            long ttl = ChronoUnit.SECONDS.between(LocalDateTime.now(), token.getExpiresTime());
            if (ttl > 0) {
                stringRedisTemplate.opsForValue().set(key, json, ttl, TimeUnit.SECONDS);
                stringRedisTemplate.opsForSet().add(
                        REFRESH_TOKEN_INDEX_PREFIX + token.getRefreshToken(),
                        token.getAccessToken());
                stringRedisTemplate.expire(
                        REFRESH_TOKEN_INDEX_PREFIX + token.getRefreshToken(),
                        Duration.ofSeconds(ttl + 60));
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize access token to JSON", e);
        }
    }

    /**
     * 从缓存获取 Access Token
     */
    public OAuth2AccessTokenDO get(String accessToken) {
        try {
            String json = stringRedisTemplate.opsForValue().get(KEY_PREFIX + accessToken);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, OAuth2AccessTokenDO.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize access token from JSON", e);
            return null;
        }
    }

    /**
     * 删除单个 Access Token 缓存
     */
    public void delete(String accessToken) {
        stringRedisTemplate.delete(KEY_PREFIX + accessToken);
    }

    /**
     * 删除与 Refresh Token 关联的所有 Access Token 缓存
     */
    public void deleteByRefreshToken(String refreshToken) {
        Set<String> tokens = stringRedisTemplate.opsForSet()
                .members(REFRESH_TOKEN_INDEX_PREFIX + refreshToken);
        if (tokens != null && !tokens.isEmpty()) {
            tokens.forEach(token -> stringRedisTemplate.delete(KEY_PREFIX + token));
        }
        stringRedisTemplate.delete(REFRESH_TOKEN_INDEX_PREFIX + refreshToken);
    }
}
