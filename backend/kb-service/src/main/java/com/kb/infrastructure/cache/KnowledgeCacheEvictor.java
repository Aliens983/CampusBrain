package com.kb.infrastructure.cache;

import com.kb.domain.event.DocumentProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 知识变更 → 问答缓存淘汰的统一入口（12-04）。
 * <p>
 * 此前问答缓存只在预约变更事件时淘汰，文档上传/重处理/删除后：
 * 精确缓存（L1 Caffeine 5min / L2 Redis 1h）与语义缓存（Qdrant 24h）里
 * 仍可能返回引用已删文档的旧答案。现统一收口：
 * <ul>
 *   <li>文档处理成功（READY，含首次上传与强制重处理）→ 全量淘汰；</li>
 *   <li>文档删除事务提交后 → 由应用服务调用 {@link #evictAllQaCaches(String)}；</li>
 *   <li>预约变更 → 仍由 MQ 消费者各自淘汰（实时余量语义不同，保持显式调用）。</li>
 * </ul>
 * 缓存全部是可重建的派生数据，淘汰失败只记日志，依赖各层 TTL 兜底，
 * 绝不向调用方（事件发布/删除主流程）抛异常。
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeCacheEvictor {

    private final QaCacheService qaCacheService;
    private final SemanticCacheService semanticCacheService;

    /**
     * 文档处理完成（READY）后淘汰问答缓存。
     * FAILED 不淘汰：文档内容未变化，无需牺牲缓存命中率。
     */
    @EventListener
    public void onDocumentProcessed(DocumentProcessedEvent event) {
        if (!event.isReady()) {
            return;
        }
        evictAllQaCaches("文档处理完成 documentId=" + event.getDocumentId());
    }

    /**
     * 全量淘汰两级问答缓存（精确 + 语义）。
     *
     * @param reason 淘汰原因，仅用于日志
     */
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
