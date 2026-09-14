package com.kb.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kb.domain.conversation.Conversation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    /**
     * 问答缓存总开关，默认关闭。
     * <p>
     * 缓存写入/读取此前从未被调用（死 Bean），但预约变更事件会调 evictAll()——
     * 后者内部对 qa:cache:* 做 Redis SCAN，在没有缓存可清时是纯空转开销。
     * 这里加开关：关闭时所有方法短路，既消除空转，也让"缓存是否启用"成为显式配置。
     * <p>
     * 为何默认关闭（详见 docs/为什么不用缓存.md）：预约类问答依赖实时余量，
     * 缓存会导致用户看到过期名额；且同一问题的答案可能来自 Function Calling，
     * 缓存后无法区分。启用前需先解决"仅对纯知识类问题缓存"的判定。
     */
    @Value("${kb.cache.enabled:false}")
    private boolean cacheEnabled;

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
        if (!cacheEnabled) {
            return;
        }
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
        if (!cacheEnabled) {
            return Optional.empty();
        }
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

    /**
     * 清空问答缓存（L1 本地 + L2 Redis）。
     * <p>
     * 3.1.1：预约数据（余量/可约状态/我的预约）变更后，旧的问答缓存可能已过期，
     * 由预约变更事件调用本方法联动失效。问答缓存全部为可重建的派生数据，直接整体淘汰即可。
     */
    public void evictAll() {
        localCache.invalidateAll();
        // 缓存关闭时 Redis 里根本不会有 qa:cache:* 键，
        // 但 SCAN 仍会执行——每次预约变更都空转一次。短路掉。
        if (!cacheEnabled) {
            return;
        }
        String pattern = "qa:cache:*";
        java.util.Set<String> keys = new java.util.HashSet<>();
        try (var cursor = stringRedisTemplate.scan(
                org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match(pattern)
                        .count(200)
                        .build())) {
            cursor.forEachRemaining(keys::add);
        }
        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
            log.info("预约变更联动：已清空精确问答缓存 {} 条", keys.size());
        }
    }
}
