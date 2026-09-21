package com.kb.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * QaCacheService 两级缓存语义测试（A-05：L1 退化为短 TTL 读缓存，写路径以 L2 Redis 为准）。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("问答精确缓存两级缓存测试")
class QaCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private ZSetOperations<String, String> zsetOps;

    private final CacheKeyBuilder keyBuilder = new CacheKeyBuilder();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private QaCacheService qaCacheService;

    @BeforeEach
    void setUp() {
        // lenient：开关关闭用例不触发 Redis，opsForValue 桩不会被用到
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zsetOps);
        qaCacheService = new QaCacheService(redisTemplate, keyBuilder, objectMapper);
        ReflectionTestUtils.setField(qaCacheService, "cacheEnabled", true);
    }

    @Test
    @DisplayName("写路径只写 L2：缓存写入后不会再预填 L1，L2 miss 时整体未命中")
    void cacheAnswer_writesOnlyRedis_noLocalPriming() {
        qaCacheService.cacheAnswer("q", "a", List.of(), null);

        verify(valueOps).set(anyString(), anyString(), any(Duration.class));
        // L2 未命中 → 返回空，证明写入未把 L1 预填（否则会 L1 hit）
        when(valueOps.get(anyString())).thenReturn(null);
        assertThat(qaCacheService.getCachedAnswer("q", null)).isEmpty();
    }

    @Test
    @DisplayName("L2 命中回填 L1（read-through）：第二次读取只查本地，不再回源 Redis")
    void l2Hit_backfillsL1_secondReadIsLocalOnly() throws Exception {
        String json = objectMapper.writeValueAsString(
                new QaCacheService.QaCacheEntry("a", List.of(), 1L, null));
        when(valueOps.get(anyString())).thenReturn(json);

        Optional<QaCacheService.QaCacheEntry> first = qaCacheService.getCachedAnswer("q", null);
        assertThat(first).isPresent();
        assertThat(first.get().answer()).isEqualTo("a");

        Optional<QaCacheService.QaCacheEntry> second = qaCacheService.getCachedAnswer("q", null);
        assertThat(second).isPresent();

        // 两次读取但 Redis 只回源一次，第二次走 L1
        verify(valueOps, times(1)).get(anyString());
    }

    @Test
    @DisplayName("evictAll 清空 L1 后下次读取重新回源 L2")
    void evictAll_clearsL1_forcesRedisReread() throws Exception {
        String json = objectMapper.writeValueAsString(
                new QaCacheService.QaCacheEntry("a", List.of(), 1L, null));
        when(valueOps.get(anyString())).thenReturn(json);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(mock(Cursor.class));

        qaCacheService.getCachedAnswer("q", null); // 回填 L1（第 1 次回源）
        qaCacheService.evictAll();           // 清空 L1
        qaCacheService.getCachedAnswer("q", null); // L1 已清 → 第 2 次回源

        verify(valueOps, times(2)).get(anyString());
    }

    @Test
    @DisplayName("开关关闭：读写与 evictAll 全短路，不触碰 Redis")
    void disabled_shortCircuitsAllOperations() {
        ReflectionTestUtils.setField(qaCacheService, "cacheEnabled", false);

        qaCacheService.cacheAnswer("q", "a", List.of(), null);
        assertThat(qaCacheService.getCachedAnswer("q", null)).isEmpty();
        qaCacheService.evictAll();

        verify(redisTemplate, never()).opsForValue();
        verify(redisTemplate, never()).scan(any(ScanOptions.class));
    }

    @Test
    @DisplayName("Redis 反序列化失败：降级为未命中，不抛异常不污染 L1")
    void l2CorruptJson_treatsAsMiss() throws Exception {
        when(valueOps.get(anyString())).thenReturn("{not-json");

        assertThat(qaCacheService.getCachedAnswer("q", null)).isEmpty();
    }

    @Test
    @DisplayName("P1-02：有主条目仅本人可见，他人读取视为未命中（含已回填的 L1）")
    void ownedEntryNotVisibleToOtherUsers() throws Exception {
        String json = objectMapper.writeValueAsString(
                new QaCacheService.QaCacheEntry("a", List.of(), 1L, 100L));
        when(valueOps.get(anyString())).thenReturn(json);

        // 本人可见
        assertThat(qaCacheService.getCachedAnswer("q", 100L)).isPresent();
        // 他人不可见：此时 L1 已被上一次回填，必须同样被归属过滤挡下，
        // 否则修复 P1-01（私有文档可被召回）后，私有答案会经 L1 泄给其他用户。
        assertThat(qaCacheService.getCachedAnswer("q", 200L)).isEmpty();
    }

    @Test
    @DisplayName("P1-02：无主（共享）条目对任何用户可见")
    void ownerlessEntryVisibleToEveryone() throws Exception {
        String json = objectMapper.writeValueAsString(
                new QaCacheService.QaCacheEntry("a", List.of(), 1L, null));
        when(valueOps.get(anyString())).thenReturn(json);

        assertThat(qaCacheService.getCachedAnswer("q", 100L)).isPresent();
        assertThat(qaCacheService.getCachedAnswer("q", 200L)).isPresent();
    }

    @Test
    @DisplayName("P1-03：Redis 连接异常时降级为未命中，不向上抛（缓存不是硬依赖）")
    void redisConnectionFailureDegradesToMiss() {
        when(valueOps.get(anyString())).thenThrow(
                new org.springframework.data.redis.RedisConnectionFailureException("redis down"));

        // 修复前：只 catch JsonProcessingException，此处异常会穿透到问答兜底文案，
        // 检索与 LLM 根本不会执行。
        assertThat(qaCacheService.getCachedAnswer("q", 100L)).isEmpty();
    }
}