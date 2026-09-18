package com.kb.infrastructure.schedule;

import com.kb.domain.document.Document;
import com.kb.domain.document.DocumentRepository;
import com.kb.domain.document.DocumentStatus;
import com.kb.infrastructure.mq.DocumentProcessingProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 卡死文档超时回收任务（12-03）。
 * <p>
 * 背景：文档进入中间态（UPLOADED/PARSING/CHUNKING/EMBEDDING）后，若消费者宕机、
 * 消息丢失或重试耗尽进 DLQ，文档会永久停在"处理中"，用户只能手动重处理。
 * <p>
 * 机制：周期性扫描 updated_at 早于阈值的中间态文档，经 Redis reclaim 锁去重后
 * 重新投递处理消息（多实例部署下只有一个实例会重投同一文档）。消费者侧的
 * processing 锁保证不会与仍存活的处理并发。
 * <p>
 * 阈值必须显著大于正常处理时长（大文件解析 + embedding），避免误回收。
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StuckDocumentReclaimer {

    private static final List<DocumentStatus> STUCK_STATUSES = List.of(
            DocumentStatus.UPLOADED,
            DocumentStatus.PARSING,
            DocumentStatus.CHUNKING,
            DocumentStatus.EMBEDDING);

    private final DocumentRepository documentRepository;
    private final DocumentProcessingProducer documentProcessingProducer;
    private final DocumentProcessingLock processingLock;

    /** 判定卡死的停留分钟数（updated_at 早于 now - N 分钟） */
    @Value("${kb.document.reclaim.stale-minutes:10}")
    private int staleMinutes;

    /** 每轮单状态最多捞取条数 */
    @Value("${kb.document.reclaim.batch-size:50}")
    private int batchSize;

    /** reclaim 去重锁 TTL：覆盖一个扫描周期，防止多实例/连续两轮重复重投 */
    @Value("${kb.document.reclaim.lock-ttl-seconds:120}")
    private long reclaimLockTtlSeconds;

    /**
     * 每 5 分钟扫描一次（启动 2 分钟后开始，避开启动风暴）。
     */
    @Scheduled(fixedDelayString = "${kb.document.reclaim.interval-ms:300000}",
            initialDelayString = "${kb.document.reclaim.initial-delay-ms:120000}")
    public void reclaimStuckDocuments() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(staleMinutes);
        List<Document> stuck;
        try {
            stuck = documentRepository.findStuckInProcessing(STUCK_STATUSES, threshold, batchSize);
        } catch (Exception e) {
            log.warn("扫描卡死文档失败，跳过本轮", e);
            return;
        }
        if (stuck.isEmpty()) {
            return;
        }

        int redispatched = 0;
        for (Document doc : stuck) {
            Long id = doc.getId();
            try {
                // 多实例/多轮去重：拿不到 reclaim 锁说明其他实例刚重投过
                if (!processingLock.acquireForReclaim(id, Duration.ofSeconds(reclaimLockTtlSeconds))) {
                    continue;
                }
                documentProcessingProducer.send(id, true);
                redispatched++;
                log.warn("检测到卡在 {} 状态的文档（updatedAt={}），已重新投递处理: id={}",
                        doc.getStatus(), doc.getUpdatedAt(), id);
            } catch (Exception e) {
                log.warn("重新投递卡死文档失败: id={}", id, e);
            }
        }
        if (redispatched > 0) {
            log.info("卡死文档回收完成：本轮扫描 {} 篇，重新投递 {} 篇", stuck.size(), redispatched);
        }
    }
}
