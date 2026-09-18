package com.kb.infrastructure.schedule;

import com.kb.domain.document.DocumentIndexCleaner;
import com.kb.domain.document.IndexDeleteFailure;
import com.kb.domain.document.IndexDeleteFailureRepository;
import com.kb.domain.document.IndexTarget;
import com.kb.domain.rag.VectorStoreService;
import com.kb.infrastructure.metrics.BusinessMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 外部索引删除失败补偿对账任务（A-04）。
 * <p>
 * 文档删除走"先提交 DB、后清外部存储"，Qdrant/ES 当时不可用会在
 * {@code index_delete_failure} 留 PENDING 记录。本任务周期性重试删除：
 * <ul>
 *   <li>重试成功 → RESOLVED + recovered 指标，孤儿记录清除；</li>
 *   <li>仍失败 → retry_count +1，下轮再试；</li>
 *   <li>达到上限 → GIVE_UP + give_up 指标（配置告警），等待人工核查。</li>
 * </ul>
 * 单轮内每条记录只尝试一次，配合 fixedDelay 天然形成退避，避免外部存储
 * 长时间故障时被重试打满。
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kb.index-cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class IndexDeleteFailureReclaimer {

    private final IndexDeleteFailureRepository failureRepository;
    private final VectorStoreService vectorStore;
    private final DocumentIndexCleaner indexCleaner;
    private final BusinessMetrics metrics;

    /** 每轮最多捞取的 PENDING 记录数 */
    @Value("${kb.index-cleanup.batch-size:100}")
    private int batchSize;

    /** 最大调度器重试次数：达到后置 GIVE_UP 并告警 */
    @Value("${kb.index-cleanup.max-retries:24}")
    private int maxRetries;

    @Scheduled(fixedDelayString = "${kb.index-cleanup.interval-ms:300000}",
            initialDelayString = "${kb.index-cleanup.initial-delay-ms:180000}")
    public void compensatePendingFailures() {
        List<IndexDeleteFailure> pending;
        try {
            pending = failureRepository.findPending(maxRetries, batchSize);
        } catch (Exception e) {
            log.warn("扫描外部索引删除失败记录失败，跳过本轮", e);
            return;
        }
        if (pending.isEmpty()) {
            return;
        }

        int recovered = 0;
        int giveUp = 0;
        for (IndexDeleteFailure failure : pending) {
            Long documentId = failure.getDocumentId();
            IndexTarget target = failure.getTarget();
            try {
                retryDelete(documentId, target);
                failureRepository.markResolved(failure.getId());
                metrics.recordIndexCleanupRecovered(target.name());
                recovered++;
                log.info("外部索引删除补偿成功，孤儿记录已清除: id={}, documentId={}, target={}",
                        failure.getId(), documentId, target);
            } catch (Exception e) {
                int attempts = failure.getRetryCount() + 1;
                String reason = e.getClass().getSimpleName() + ": " + e.getMessage();
                failureRepository.recordOrIncrement(documentId, target, reason);
                if (attempts >= maxRetries) {
                    failureRepository.markGiveUp(failure.getId());
                    metrics.recordIndexCleanupGiveUp(target.name());
                    giveUp++;
                    log.error("外部索引删除补偿达到重试上限 {}，置 GIVE_UP 待人工核查: documentId={}, target={}",
                            maxRetries, documentId, target, e);
                } else {
                    log.debug("外部索引删除补偿仍失败，留待下轮: documentId={}, target={}, attempts={}",
                            documentId, target, attempts);
                }
            }
        }
        if (recovered > 0 || giveUp > 0) {
            log.info("外部索引删除对账本轮完成：扫描 {} 条，恢复 {} 条，放弃 {} 条",
                    pending.size(), recovered, giveUp);
        }
    }

    private void retryDelete(Long documentId, IndexTarget target) {
        String idStr = String.valueOf(documentId);
        switch (target) {
            case QDRANT -> vectorStore.deleteByDocumentId(idStr);
            case ELASTICSEARCH -> indexCleaner.deleteByDocumentId(idStr);
        }
    }
}
