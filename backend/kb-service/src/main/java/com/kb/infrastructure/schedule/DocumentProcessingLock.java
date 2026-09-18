package com.kb.infrastructure.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档处理相关的 Redis 短锁（12-03）。
 * <p>
 * 两类锁，均基于 SET NX EX，宕机时靠 TTL 自动释放：
 * <ul>
 *   <li><b>processing 锁</b>：消费者开始处理时占用。同一文档的重复消息（本地重试、
 *       超时回收重投、手工重放）只有一个真正执行者，避免两个消费者并发
 *       "删旧向量 → 写新向量"互相踩踏。锁值为本 JVM 生成的 owner token，
 *       因此同一条消息在本 JVM 内的 Spring retry 重试、以及 TTL 窗口内重新投递回本 JVM，
 *       都能被识别为"持有者本人"而放行；TTL 过期后 token 对不上（或键已不存在）则不再放行；</li>
 *   <li><b>reclaim 锁</b>：超时回收任务重投前占用，保证多实例部署下只有一个实例
 *       对同一文档执行重投，避免每轮扫描重复发消息。</li>
 * </ul>
 * Redis 不可用时采取保守策略：获取失败视为"已被占用"（宁可本轮不处理，也不并发）。
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingLock {

    private static final String PROCESSING_KEY_PREFIX = "lock:document:processing:";
    private static final String RECLAIM_KEY_PREFIX = "lock:document:reclaim:";

    private final StringRedisTemplate stringRedisTemplate;

    /** 本 JVM 对每个文档持有的 owner token，识别"本地重试 / TTL 内回到本实例的重投递" */
    private final Map<Long, String> processingTokens = new ConcurrentHashMap<>();

    /**
     * 进入文档处理：尝试占用处理锁；若锁已被本 JVM 持有（本地重试 / TTL 内重投递回本实例），
     * 同样返回 true。
     *
     * @return true 表示当前调用可以执行处理；false 表示其他实例正在处理（应 ACK 跳过）
     */
    public boolean enterProcessing(Long documentId, Duration ttl) {
        String token = processingTokens.computeIfAbsent(documentId,
                k -> UUID.randomUUID().toString());
        try {
            Boolean ok = stringRedisTemplate.opsForValue()
                    .setIfAbsent(PROCESSING_KEY_PREFIX + documentId, token, ttl);
            if (Boolean.TRUE.equals(ok)) {
                return true;
            }
            // 锁已存在：只有持有者是本 JVM（token 匹配）才放行，
            // TTL 过期被别人抢走时 token 不匹配，本实例自动退让
            String current = stringRedisTemplate.opsForValue()
                    .get(PROCESSING_KEY_PREFIX + documentId);
            return token.equals(current);
        } catch (Exception e) {
            // Redis 故障时保守拒绝，避免在无锁保护下并发处理
            log.warn("获取文档处理锁失败，保守跳过: id={}", documentId, e);
            return false;
        }
    }

    /** 终态退出（READY/永久 FAILED）：仅当锁仍属于本 JVM 时删除，并清理本地 token */
    public void exitProcessing(Long documentId) {
        String token = processingTokens.remove(documentId);
        if (token == null) {
            return;
        }
        try {
            String key = PROCESSING_KEY_PREFIX + documentId;
            String current = stringRedisTemplate.opsForValue().get(key);
            if (token.equals(current)) {
                stringRedisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.debug("释放文档处理锁失败（依赖 TTL 兜底）: id={}", documentId, e);
        }
    }

    /**
     * 尝试占用"回收重投"锁。
     *
     * @return true 表示本实例获得本轮重投权
     */
    public boolean acquireForReclaim(Long documentId, Duration ttl) {
        try {
            Boolean ok = stringRedisTemplate.opsForValue()
                    .setIfAbsent(RECLAIM_KEY_PREFIX + documentId, "1", ttl);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.warn("获取文档回收锁失败，保守跳过: id={}", documentId, e);
            return false;
        }
    }
}
