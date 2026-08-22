package com.kb.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kb.domain.conversation.Conversation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 问答缓存服务 — 两级缓存
 * <p>
 * L1（Caffeine 本地）：纳秒级，存热点问题 Top-100，TTL 5 分钟
 * L2（Redis 分布式）：毫秒级，存所有问答，TTL 1 小时
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Service
public class QaCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final CacheKeyBuilder keyBuilder;
    private final ObjectMapper objectMapper;

    /** L1 本地缓存：最多 100 条，5 分钟过期 */
    private final Cache<String, QaCacheEntry> localCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    /** L2 Redis TTL */
    private static final Duration REDIS_TTL = Duration.ofHours(1);

    public QaCacheService(StringRedisTemplate stringRedisTemplate,
                          CacheKeyBuilder keyBuilder,
                          ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.keyBuilder = keyBuilder;
        this.objectMapper = objectMapper;
    }

    /**
     * 写入问答缓存（L1 + L2 双写）
     */
    public void cacheAnswer(String query, String answer,
                             List<Conversation.CitationRef> citations) {
        QaCacheEntry entry = new QaCacheEntry(answer, citations, System.currentTimeMillis());
        String key = keyBuilder.qaCacheKey(query);

        localCache.put(key, entry);
        try {
            stringRedisTemplate.opsForValue().set(key,
                    objectMapper.writeValueAsString(entry), REDIS_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache QA to Redis", e);
        }
    }

    /**
     * 查询缓存（L1 → L2 → 回填）
     */
    public Optional<QaCacheEntry> getCachedAnswer(String query) {
        String key = keyBuilder.qaCacheKey(query);

        QaCacheEntry local = localCache.getIfPresent(key);
        if (local != null) {
            log.debug("L1 cache hit");
            return Optional.of(local);
        }

        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                QaCacheEntry entry = objectMapper.readValue(json, QaCacheEntry.class);
                localCache.put(key, entry);
                stringRedisTemplate.opsForZSet().incrementScore(
                        keyBuilder.hotQueriesKey(), query, 1);
                log.debug("L2 cache hit, backfilled L1");
                return Optional.of(entry);
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize cached QA", e);
        }
        return Optional.empty();
    }

    /**
     * 缓存实体
     */
    public record QaCacheEntry(String answer,
                               List<Conversation.CitationRef> citations,
                               long cachedAt) {}
}
