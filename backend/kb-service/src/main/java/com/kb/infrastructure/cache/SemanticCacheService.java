package com.kb.infrastructure.cache;

import com.kb.domain.rag.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 语义缓存服务 — 基于向量相似度的问答缓存。
 * <p>
 * 与精确匹配缓存互补：当用户问题字面不同但语义相似时，
 * 通过向量余弦相似度匹配命中缓存，减少 LLM 重复调用。
 * </p>
 * <p>
 * 实现方式：
 * <ul>
 *   <li>将问题向量化后与 Redis 中已缓存的向量进行余弦相似度计算</li>
 *   <li>相似度 &gt;= 阈值（默认 0.95）视为命中</li>
 *   <li>缓存值存储序列化的 {@code CachedAnswer}</li>
 * </ul>
 * </p>
 * @author forever-king
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final EmbeddingService embeddingService;

    /** Redis key 前缀 */
    private static final String PREFIX = "semantic:qa:";
    /** 默认相似度阈值 */
    private static final double SIMILARITY_THRESHOLD = 0.95;
    /** 缓存过期时间（小时） */
    private static final int TTL_HOURS = 24;

    /**
     * 查找语义缓存。
     *
     * @param question 用户问题
     * @return 命中的缓存答案，未命中返回 null
     */
    public String lookup(String question) {
        try {
            float[] queryVec = embeddingService.embed(question);
            // 用 SCAN 代替 KEYS，避免阻塞 Redis
            var keys = new java.util.HashSet<String>();
            try (var cursor = redisTemplate.scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions()
                            .match(PREFIX + "*")
                            .count(100)
                            .build())) {
                cursor.forEachRemaining(keys::add);
            }
            if (keys.isEmpty()) return null;

            for (String key : keys) {
                String cachedVecStr = redisTemplate.opsForValue().get(key + ":vec");
                String cachedAnswer = redisTemplate.opsForValue().get(key + ":ans");
                if (cachedVecStr == null || cachedAnswer == null) continue;

                float[] cachedVec = deserializeVector(cachedVecStr);
                double similarity = cosineSimilarity(queryVec, cachedVec);
                if (similarity >= SIMILARITY_THRESHOLD) {
                    log.info("Semantic cache HIT (sim={}): [{}]", String.format("%.3f", similarity), question);
                    return cachedAnswer;
                }
            }
            log.debug("Semantic cache MISS: [{}]", question);
            return null;
        } catch (Exception e) {
            log.warn("Semantic cache lookup failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 存入语义缓存。
     */
    public void store(String question, String answer) {
        try {
            float[] vec = embeddingService.embed(question);
            String key = PREFIX + question.hashCode();
            redisTemplate.opsForValue().set(key + ":vec", serializeVector(vec), TTL_HOURS, TimeUnit.HOURS);
            redisTemplate.opsForValue().set(key + ":ans", answer, TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Semantic cache store failed: {}", e.getMessage());
        }
    }

    /**
     * 余弦相似度计算。
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String serializeVector(float[] vec) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vec[i]);
        }
        return sb.toString();
    }

    private float[] deserializeVector(String str) {
        String[] parts = str.split(",");
        float[] vec = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vec[i] = Float.parseFloat(parts[i]);
        }
        return vec;
    }
}
