package com.kb.infrastructure.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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
 * <p>
 * 3.9（深度审查 P1）加固：
 * <ul>
 *   <li><b>双判据</b>：除 Redis token 外，本 JVM 维护"在途任务登记表"（{@link #inFlightTasks}）。
 *       token 匹配且本机已有该文档的在途处理 → 判定为"TTL 窗口内重投递回本实例的重复消息"，
 *       直接确认跳过，杜绝与首次处理并发执行"删旧→写新"；token 匹配但在途表无登记
 *       → 本地 Spring retry 重试，放行并重新登记。</li>
 *   <li><b>锁续约（watchdog）</b>：定时刷新本机在途文档的 Redis 锁 TTL，
 *       正常处理超过锁 TTL 不再中途失锁，跨实例并发窗口被彻底关闭。</li>
 *   <li><b>登记表清理</b>：终态/可重试退出后注销在途登记；定时剪除长期无活动条目，
 *       防止 token 与登记项随文档状态悬停只增不减。</li>
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

    /** 处理中锁 TTL：正常处理应远小于该值；消费者宕机时由 TTL 自动释放 */
    @Value("${kb.document.processing-lock-ttl-seconds:1800}")
    private long processingLockTtlSeconds;

    /** 锁续约周期（毫秒）：远小于锁 TTL，Watchdog 定期续约在途文档 */
    @Value("${kb.document.processing-lock-renew-ms:300000}")
    private long renewIntervalMs;

    /** 本机登记清理阈值（毫秒）：在途登记超过该时长无活动即视为悬停，剪除 */
    @Value("${kb.document.processing-lock-prune-after-ms:3600000}")
    private long pruneAfterMs;

    /** 本 JVM 对每个文档持有的 owner token，识别"本地重试 / TTL 内回到本实例的重投递" */
    private final Map<Long, String> processingTokens = new ConcurrentHashMap<>();

    /** 3.9 本 JVM 在途任务登记表：documentId → 最近活动时间（毫秒），用于识别"重复消息与首次处理并发" */
    private final Map<Long, Long> inFlightTasks = new ConcurrentHashMap<>();

    /**
     * 进入文档处理：尝试占用处理锁。
     * <ul>
     *   <li>Redis 锁抢占成功 → 登记本机在途，放行；</li>
     *   <li>锁被本机持有（token 匹配）：在途表中已有登记 → 判定重复消息，拒绝（跳过）；
     *       无登记（本地 Spring retry）→ 放行并重新登记；</li>
     *   <li>锁被其他实例持有 → 拒绝（ACK 跳过）；</li>
     *   <li>Redis 故障 → 保守拒绝。</li>
     * </ul>
     *
     * @return true 表示当前调用可以执行处理；false 表示应 ACK 跳过
     */
    public boolean enterProcessing(Long documentId) {
        Duration ttl = Duration.ofSeconds(processingLockTtlSeconds);
        String token = processingTokens.computeIfAbsent(documentId,
                k -> UUID.randomUUID().toString());
        try {
            String key = PROCESSING_KEY_PREFIX + documentId;
            Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, token, ttl);
            if (Boolean.TRUE.equals(ok)) {
                inFlightTasks.put(documentId, System.currentTimeMillis());
                return true;
            }
            // 锁已存在：只有持有者是本 JVM（token 匹配）才继续判断
            String current = stringRedisTemplate.opsForValue().get(key);
            if (!token.equals(current)) {
                // TTL 过期被别人抢走时 token 不匹配，本实例自动退让
                return false;
            }
            if (inFlightTasks.containsKey(documentId)) {
                // 3.9 双判据：本机已有在途任务，本次是"TTL 窗口内重投递回本实例"的重复消息，
                // 若放行会与首次处理并发"删旧→写新"产生重复分块/向量交错，直接跳过。
                log.info("文档在本实例仍有在途处理，重复消息跳过: id={}", documentId);
                return false;
            }
            // token 匹配且本机无在途任务：本地 Spring retry 重试，放行并续约重新登记
            stringRedisTemplate.opsForValue().set(key, token, ttl);
            inFlightTasks.put(documentId, System.currentTimeMillis());
            return true;
        } catch (Exception e) {
            // Redis 故障时保守拒绝，避免在无锁保护下并发处理
            log.warn("获取文档处理锁失败，保守跳过: id={}", documentId, e);
            return false;
        }
    }

    /**
     * 终态退出（READY/永久 FAILED）：仅当锁仍属于本 JVM 时删除，并清理本地 token 与在途登记。
     */
    public void exitProcessing(Long documentId) {
        // 先取 token 再移除，Redis 侧按"锁仍属于本 JVM"才删除，防止误删他人接管的锁
        String token = processingTokens.remove(documentId);
        inFlightTasks.remove(documentId);
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
     * 在途登记注销（3.9）：可重试失败向 Spring retry 抛出的 finally 中调用，
     * 使下一次重试能重新入场（token 仍在 Redis，只是取消"在途"标记）。
     * 幂等；对终态路径（已调用 {@link #exitProcessing}）无害。
     */
    public void markTaskFinished(Long documentId) {
        inFlightTasks.remove(documentId);
    }

    /**
     * 是否有其他（或本机）进程正在处理该文档：Redis 处理锁存在即视为处理中。
     * 供超时回收任务在重投前探测（3.10），避免对仍在处理的文档反复重投。
     */
    public boolean isProcessing(Long documentId) {
        try {
            return stringRedisTemplate.opsForValue()
                    .get(PROCESSING_KEY_PREFIX + documentId) != null;
        } catch (Exception e) {
            // Redis 探活失败：保守按"处理中"处理，本轮不重投
            log.debug("探测文档处理锁失败，按处理中处理: id={}", documentId);
            return true;
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

    /**
     * 3.9 锁续约 Watchdog：对"本机在途"的文档定期刷新 Redis 锁 TTL，
     * 使正常处理超过锁 TTL 也不失锁；同时更新在途登记的活动时间。
     */
    @Scheduled(fixedDelayString = "${kb.document.processing-lock-renew-ms:300000}")
    public void renewProcessingLocks() {
        if (inFlightTasks.isEmpty()) {
            return;
        }
        Duration ttl = Duration.ofSeconds(processingLockTtlSeconds);
        long now = System.currentTimeMillis();
        for (Map.Entry<Long, Long> entry : inFlightTasks.entrySet()) {
            Long documentId = entry.getKey();
            String token = processingTokens.get(documentId);
            if (token == null) {
                inFlightTasks.remove(documentId);
                continue;
            }
            try {
                String key = PROCESSING_KEY_PREFIX + documentId;
                String current = stringRedisTemplate.opsForValue().get(key);
                if (token.equals(current)) {
                    // 仍由本机持有：续约（刷新 TTL）+ 刷新活动时间
                    stringRedisTemplate.opsForValue().set(key, token, ttl);
                    inFlightTasks.put(documentId, now);
                } else {
                    // 锁已被他人接管（TTL 期间本机进程长时间阻塞/重启）：放弃本机登记
                    processingTokens.remove(documentId);
                    inFlightTasks.remove(documentId);
                }
            } catch (Exception e) {
                log.debug("续约文档处理锁失败（下轮再试）: id={}", documentId, e);
            }
        }
    }

    /**
     * 3.9 登记表剪枝：清理长期无活动的本机 token 与在途登记（文档悬停/进程异常退出后的残留），
     * 防止内存只增不减。
     */
    @Scheduled(fixedDelayString = "${kb.document.processing-lock-prune-after-ms:3600000}",
            initialDelayString = "${kb.document.processing-lock-prune-after-ms:3600000}")
    public void pruneStaleRegistrations() {
        long cutoff = System.currentTimeMillis() - pruneAfterMs;
        inFlightTasks.entrySet().removeIf(e -> e.getValue() < cutoff);
        // 在途登记被剪除的文档，其 token 一并清理
        processingTokens.keySet().removeIf(id -> !inFlightTasks.containsKey(id) && isLockMismatched(id));
    }

    private boolean isLockMismatched(Long documentId) {
        try {
            String key = PROCESSING_KEY_PREFIX + documentId;
            String token = processingTokens.get(documentId);
            String current = stringRedisTemplate.opsForValue().get(key);
            return current == null || !current.equals(token);
        } catch (Exception e) {
            return false;
        }
    }
}