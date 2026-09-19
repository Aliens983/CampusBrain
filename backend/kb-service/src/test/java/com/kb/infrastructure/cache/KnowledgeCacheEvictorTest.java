package com.kb.infrastructure.cache;

import com.kb.domain.document.DocumentStatus;
import com.kb.domain.event.DocumentProcessedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 5.5（深度审查 P1）KnowledgeCacheEvictor 单测。
 * <p>
 * 守护 3.7（P1）：<ul>
 *   <li>READY 与 FAILED 的文档处理事件都必须触发缓存淘汰（FAILED 时旧索引已被删，旧缓存答案必须失效）；</li>
 *   <li>合并窗口把 N 个文档事件合并为一次全量淘汰，避免 N 次 SCAN/delete 风暴；</li>
 *   <li>任一侧缓存淘汰抛异常都不能影响另一侧、更不能抛出给事件发布方。</li>
 * </ul>
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeCacheEvictor 合并窗口与 FAILED 淘汰")
class KnowledgeCacheEvictorTest {

    @Mock private QaCacheService qaCacheService;
    @Mock private SemanticCacheService semanticCacheService;

    private KnowledgeCacheEvictor evictor;

    @BeforeEach
    void setUp() {
        evictor = new KnowledgeCacheEvictor(qaCacheService, semanticCacheService);
    }

    private static DocumentProcessedEvent event(long docId, DocumentStatus status) {
        return new DocumentProcessedEvent(new Object(), docId, "doc-" + docId, 1L, status, null);
    }

    @Test
    @DisplayName("合并窗口：3 个 READY 事件只触发 1 次全量淘汰")
    void mergeWindow_flushesOnceForManyEvents() {
        evictor.onDocumentProcessed(event(1L, DocumentStatus.READY));
        evictor.onDocumentProcessed(event(2L, DocumentStatus.READY));
        evictor.onDocumentProcessed(event(3L, DocumentStatus.READY));

        evictor.flushPendingDocumentEvictions();

        verify(qaCacheService, times(1)).evictAll();
        verify(semanticCacheService, times(1)).evictAll();
    }

    @Test
    @DisplayName("FAILED 事件同样淘汰（3.7 回归守护：重处理失败旧索引已删，旧答案必须失效）")
    void failedEvent_alsoTriggersEviction() {
        evictor.onDocumentProcessed(event(9L, DocumentStatus.FAILED));

        evictor.flushPendingDocumentEvictions();

        verify(qaCacheService, times(1)).evictAll();
        verify(semanticCacheService, times(1)).evictAll();
    }

    @Test
    @DisplayName("合并后缓冲清空：再 flush 无事件时不再淘汰")
    void flushWithEmptyBuffer_doesNothing() {
        evictor.onDocumentProcessed(event(1L, DocumentStatus.READY));
        evictor.flushPendingDocumentEvictions();
        verify(qaCacheService, times(1)).evictAll();

        // 第二次 flush（无新事件）不应重复全量淘汰
        evictor.flushPendingDocumentEvictions();
        verify(qaCacheService, times(1)).evictAll();
        verify(semanticCacheService, times(1)).evictAll();
    }

    @Test
    @DisplayName("精确缓存淘汰抛异常：语义缓存仍执行，且不向外抛")
    void exactEvictionFailure_doesNotBlockSemanticNorPropagate() {
        doThrow(new RuntimeException("redis down")).when(qaCacheService).evictAll();

        evictor.evictAllQaCaches("测试");

        verify(semanticCacheService).evictAll();
    }

    @Test
    @DisplayName("语义缓存淘汰抛异常：不向外抛（依赖 TTL 兜底）")
    void semanticEvictionFailure_doesNotPropagate() {
        doThrow(new RuntimeException("qdrant down")).when(semanticCacheService).evictAll();

        evictor.evictAllQaCaches("测试");

        verify(qaCacheService).evictAll();
    }

    @Test
    @DisplayName("直接调用 evictAllQaCaches（文档删除路径）：两级都淘汰")
    void directEvictAll_evictsBothTiers() {
        evictor.evictAllQaCaches("文档删除");

        verify(qaCacheService).evictAll();
        verify(semanticCacheService).evictAll();
    }

    @Test
    @DisplayName("无事件到达时 flush 不调用任何淘汰（never 守护）")
    void noEvents_flushDoesNothingAtAll() {
        evictor.flushPendingDocumentEvictions();

        verify(qaCacheService, never()).evictAll();
        verify(semanticCacheService, never()).evictAll();
    }
}