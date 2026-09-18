package com.kb.infrastructure.schedule;

import com.kb.domain.document.DocumentIndexCleaner;
import com.kb.domain.document.IndexDeleteFailure;
import com.kb.domain.document.IndexDeleteFailureRepository;
import com.kb.domain.document.IndexTarget;
import com.kb.domain.rag.VectorStoreService;
import com.kb.infrastructure.metrics.BusinessMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link IndexDeleteFailureReclaimer} 补偿对账逻辑测试（A-04）。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("外部索引删除失败补偿对账测试")
class IndexDeleteFailureReclaimerTest {

    @Mock private IndexDeleteFailureRepository failureRepository;
    @Mock private VectorStoreService vectorStore;
    @Mock private DocumentIndexCleaner indexCleaner;
    @Mock private BusinessMetrics metrics;

    private IndexDeleteFailureReclaimer reclaimer;

    @BeforeEach
    void setUp() {
        reclaimer = new IndexDeleteFailureReclaimer(
                failureRepository, vectorStore, indexCleaner, metrics);
        ReflectionTestUtils.setField(reclaimer, "batchSize", 100);
        ReflectionTestUtils.setField(reclaimer, "maxRetries", 3);
    }

    private IndexDeleteFailure pending(Long id, Long documentId, IndexTarget target, int retryCount) {
        IndexDeleteFailure f = new IndexDeleteFailure(
                documentId, target, "boom", retryCount, "PENDING");
        f.setId(id);
        return f;
    }

    @Test
    @DisplayName("重试成功：Qdrant 删除通过 → 标记 RESOLVED 并打 recovered 指标")
    void recoveredOnSuccess() {
        when(failureRepository.findPending(3, 100))
                .thenReturn(List.of(pending(1L, 10L, IndexTarget.QDRANT, 0)));

        reclaimer.compensatePendingFailures();

        verify(vectorStore).deleteByDocumentId("10");
        verify(indexCleaner, never()).deleteByDocumentId(anyString());
        verify(failureRepository).markResolved(1L);
        verify(failureRepository, never()).markGiveUp(1L);
        verify(metrics).recordIndexCleanupRecovered("QDRANT");
    }

    @Test
    @DisplayName("重试仍失败且未到上限：累加失败记录，不放弃、不打 give_up 指标")
    void retriesWhenUnderLimit() {
        when(failureRepository.findPending(3, 100))
                .thenReturn(List.of(pending(2L, 20L, IndexTarget.ELASTICSEARCH, 1)));
        org.mockito.Mockito.doThrow(new RuntimeException("ES down"))
                .when(indexCleaner).deleteByDocumentId("20");

        reclaimer.compensatePendingFailures();

        verify(failureRepository).recordOrIncrement(eq(20L), eq(IndexTarget.ELASTICSEARCH),
                contains("ES down"));
        verify(failureRepository, never()).markResolved(2L);
        verify(failureRepository, never()).markGiveUp(2L);
        verify(metrics, never()).recordIndexCleanupGiveUp(anyString());
    }

    @Test
    @DisplayName("最后一次尝试仍失败：标记 GIVE_UP 并打 give_up 告警指标")
    void givesUpAtLimit() {
        when(failureRepository.findPending(3, 100))
                .thenReturn(List.of(pending(3L, 30L, IndexTarget.QDRANT, 2)));
        org.mockito.Mockito.doThrow(new IllegalStateException("Qdrant down"))
                .when(vectorStore).deleteByDocumentId("30");

        reclaimer.compensatePendingFailures();

        verify(failureRepository).recordOrIncrement(eq(30L), eq(IndexTarget.QDRANT),
                contains("Qdrant down"));
        verify(failureRepository).markGiveUp(3L);
        verify(failureRepository, never()).markResolved(3L);
        verify(metrics).recordIndexCleanupGiveUp("QDRANT");
    }

    @Test
    @DisplayName("无待补偿记录：不执行任何删除")
    void noopWhenEmpty() {
        when(failureRepository.findPending(anyInt(), anyInt())).thenReturn(List.of());

        reclaimer.compensatePendingFailures();

        verify(vectorStore, never()).deleteByDocumentId(anyString());
        verify(indexCleaner, never()).deleteByDocumentId(anyString());
    }
}
