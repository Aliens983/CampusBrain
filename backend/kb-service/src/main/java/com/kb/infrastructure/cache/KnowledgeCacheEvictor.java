package com.kb.infrastructure.cache;

import com.kb.domain.document.KnowledgeCacheInvalidator;
import com.kb.domain.event.DocumentProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识变更 → 问答缓存淘汰的统一入口（12-04）。
 * <p>
 * 此前问答缓存只在预约变更事件时淘汰，文档上传/重处理/删除后：
 * 精确缓存（L1 Caffeine 5min / L2 Redis 1h）与语义缓存（Qdrant 24h）里
 * 仍可能返回引用已删文档的旧答案。现统一收口：
 * <ul>
 *   <li>文档处理完成（READY，含首次上传与强制重处理）→ 合并窗口内统一淘汰；</li>
 *   <li>文档处理失败（3.7：重处理失败时消费端 Step0 已删旧索引，缓存旧答案必须失效）→ 同样淘汰；</li>
 *   <li>文档删除事务提交后 → 由应用服务经 {@link KnowledgeCacheInvalidator} 调用 {@link #evictAllQaCaches(String)} 立即淘汰；</li>
 *   <li>预约变更 → 仍由 MQ 消费者各自淘汰（实时余量语义不同，保持显式调用）。</li>
 * </ul>
 * 2.5（深度审查 P2）：本类直接 implements {@link KnowledgeCacheInvalidator}
 * （基础设施实现领域端口），已删除纯委托的 adapter 转发层。
 * 缓存全部是可重建的派生数据，淘汰失败只记日志，依赖各层 TTL 兜底，
 * 绝不向调用方（事件发布/删除主流程）抛异常。
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeCacheEvictor implements KnowledgeCacheInvalidator {

    private final QaCacheService qaCacheService;
    private final SemanticCacheService semanticCacheService;

    /**
     * 3.7（深度审查 P1）合并窗口：文档处理事件（READY/FAILED）先入缓冲，
     * 由定时任务每窗口合并成一次全量淘汰。批量上传 N 篇 = 1 次
     * 「SCAN + 全量 delete」，避免 N 次全量失效导致的命中率归零与 IO 风暴。
     */
    private final List<Long> pendingDocumentIds = new ArrayList<>();
    private final Object mergeLock = new Object();

    /**
     * 文档处理事件（成功 READY / 失败 FAILED）均触发问答缓存淘汰。
     * 3.7 修正：FAILED 不再跳过——消费端 Step0 已删旧向量/ES，1h/24h 缓存里
     * 仍引用该文档的旧答案必须失效。
     */
    @EventListener
    public void onDocumentProcessed(DocumentProcessedEvent event) {
        synchronized (mergeLock) {
            pendingDocumentIds.add(event.getDocumentId());
        }
        log.debug("文档处理事件入合并窗口: status={}, documentId={}",
                event.isReady() ? "READY" : "FAILED", event.getDocumentId());
    }

    /**
     * 合并窗口定时冲刷：窗口内有文档事件才执行一次全量淘汰。
     */
    @Scheduled(
            fixedDelayString = "${kb.cache.evict-merge-interval-ms:30000}",
            initialDelayString = "${kb.cache.evict-merge-init-delay-ms:60000}")
    public void flushPendingDocumentEvictions() {
        List<Long> batch;
        synchronized (mergeLock) {
            if (pendingDocumentIds.isEmpty()) {
                return;
            }
            batch = new ArrayList<>(pendingDocumentIds);
            pendingDocumentIds.clear();
        }
        String reason = "文档处理合并窗口(" + batch.size() + " 篇, 首批=" + batch.get(0) + ")";
        evictAllQaCaches(reason);
    }

    /**
     * 全量淘汰两级问答缓存（精确 + 语义）。
     * 文档删除路径由应用服务经 {@link KnowledgeCacheInvalidator} 同步调用（单次删除即一次淘汰，无风暴问题）。
     *
     * @param reason 淘汰原因，仅用于日志
     */
    @Override
    public void evictAllQaCaches(String reason) {
        try {
            qaCacheService.evictAll();
        } catch (Exception e) {
            log.warn("精确问答缓存淘汰失败（{}），依赖 TTL 兜底", reason, e);
        }
        try {
            semanticCacheService.evictAll();
        } catch (Exception e) {
            log.warn("语义问答缓存淘汰失败（{}），依赖 TTL 兜底", reason, e);
        }
        log.info("知识变更联动问答缓存淘汰完成: {}", reason);
    }
}
