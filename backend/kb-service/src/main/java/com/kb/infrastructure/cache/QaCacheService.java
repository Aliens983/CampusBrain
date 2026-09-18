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
 * 写路径以 L2（Redis）为准（单一数据源）；L1（Caffeine）退化为短 TTL 读缓存，
 * 仅在 L2 命中后回填，用于吸收热点读放大（A-05：消除 L1/L2 双写非原子窗口）。
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

    /**
     * L1 本地读缓存：最多 100 条，短 TTL 30 秒自愈。
     * 不在写路径同步写入（A-05），仅由 L2 命中回填，避免与 L2 双写非原子。
     */
    private static final Duration L1_TTL = Duration.ofSeconds(30);

    private final Cache<String, QaCacheEntry> localCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(L1_TTL)
            .build();

    /** L2 Redis TTL */
    private static final Duration REDIS_TTL = Duration.ofHours(1);

    /**
     * 问答缓存（精确缓存）开关，与语义缓存共用 {@code kb.cache.enabled}（A-06 统一语义）。
     * <p>
     * 上游已按"仅纯知识类问题才读写缓存"过滤（预约余量/档期/我的预约等实时数据
     * 绝不缓存，预约变更事件整体淘汰），因此开关默认值（true）与 application.yml 的
     * {@code KB_CACHE_ENABLED} 保持一致；实际取值以 yml 为准。关闭时所有方法短路，
     * 避免对 qa:cache:* 的 SCAN 空转。
     */
    @Value("${kb.cache.enabled:true}")
    private boolean cacheEnabled;

    public QaCacheService(StringRedisTemplate stringRedisTemplate,
                          CacheKeyBuilder keyBuilder,
                          ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.keyBuilder = keyBuilder;
        this.objectMapper = objectMapper;
    }

    /**
     * 写入问答缓存（A-05：仅写 L2 Redis，单一数据源）。
     * <p>
     * L1 只在 L2 命中时回填，写路径不再双写，天然规避 L1/L2 不一致窗口；
     * Redis 故障时本次写入整体失败，不残留 L1 中"新写失败后继续读旧值"的脏读。
     */
    public void cacheAnswer(String query, String answer,
                             List<Conversation.CitationRef> citations) {
        if (!cacheEnabled) {
            return;
        }
        QaCacheEntry entry = new QaCacheEntry(answer, citations, System.currentTimeMillis());
        String key = keyBuilder.qaCacheKey(query);

        try {
            stringRedisTemplate.opsForValue().set(key,
                    objectMapper.writeValueAsString(entry), REDIS_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache QA to Redis", e);
        }
    }

    /**
     * 查询缓存（L1 读缓存优先 → L2 为准 → 命中后回填 L1）
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
