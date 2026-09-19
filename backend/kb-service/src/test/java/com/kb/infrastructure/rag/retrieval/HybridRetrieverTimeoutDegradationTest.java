package com.kb.infrastructure.rag.retrieval;

import com.kb.domain.rag.RetrievalResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 5.5（深度审查 P1）HybridRetriever 检索超时降级测试。
 * <p>
 * 关键词（ES）/ 向量（Qdrant）任一侧在 {@code kb.retrieval.timeout-seconds} 内未返回时，
 * 该侧按空结果降级、另一侧照常融合，问答链路不抛异常、不整体失败。
 * </p>
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HybridRetriever 检索超时降级")
class HybridRetrieverTimeoutDegradationTest {

    @Mock private KeywordRetriever keywordRetriever;
    @Mock private VectorRetriever vectorRetriever;

    private HybridRetriever hybridRetriever;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        // 2 线程池：一侧阻塞时另一侧可独立返回（真实 retrievalExecutor 亦为多线程）
        executor = Executors.newFixedThreadPool(2);
        hybridRetriever = new HybridRetriever(keywordRetriever, vectorRetriever, executor);
        ReflectionTestUtils.setField(hybridRetriever, "finalTopK", 5);
        ReflectionTestUtils.setField(hybridRetriever, "rrfK", 60.0);
        // 1s 窗口：阻塞侧必然超时
        ReflectionTestUtils.setField(hybridRetriever, "retrievalTimeoutSeconds", 1);
    }

    @AfterEach
    void tearDown() {
        // 中断阻塞在 sleep 的降级侧线程
        executor.shutdownNow();
    }

    private static RetrievalResult chunk(String id) {
        return RetrievalResult.builder().chunkId(id).content("内容-" + id).build();
    }

    @Test
    @DisplayName("关键词检索超时 → 仅向量侧结果参与融合，不抛异常")
    void keywordTimeoutDegradesToVectorOnly() {
        when(keywordRetriever.retrieve(anyString())).thenAnswer(inv -> {
            Thread.sleep(60_000);
            return List.of(chunk("k1"));
        });
        when(vectorRetriever.retrieve(anyString())).thenReturn(List.of(chunk("v1"), chunk("v2")));

        List<RetrievalResult> results = hybridRetriever.hybridRetrieve("测试问题");

        assertThat(results).isNotEmpty();
        List<String> ids = results.stream().map(RetrievalResult::getChunkId).collect(Collectors.toList());
        assertThat(ids).contains("v1", "v2").doesNotContain("k1");
    }

    @Test
    @DisplayName("向量检索超时 → 仅关键词侧结果参与融合，不抛异常")
    void vectorTimeoutDegradesToKeywordOnly() {
        when(keywordRetriever.retrieve(anyString())).thenReturn(List.of(chunk("k1"), chunk("k2")));
        when(vectorRetriever.retrieve(anyString())).thenAnswer(inv -> {
            Thread.sleep(60_000);
            return List.of(chunk("v1"));
        });

        List<RetrievalResult> results = hybridRetriever.hybridRetrieve("测试问题");

        assertThat(results).isNotEmpty();
        List<String> ids = results.stream().map(RetrievalResult::getChunkId).collect(Collectors.toList());
        assertThat(ids).contains("k1", "k2").doesNotContain("v1");
    }

    @Test
    @DisplayName("两侧均超时 → 返回空列表，不抛异常（空召回走 LLM 兜底）")
    void bothTimeoutReturnEmptyWithoutException() {
        when(keywordRetriever.retrieve(anyString())).thenAnswer(inv -> {
            Thread.sleep(60_000);
            return List.of(chunk("k1"));
        });
        when(vectorRetriever.retrieve(anyString())).thenAnswer(inv -> {
            Thread.sleep(60_000);
            return List.of(chunk("v1"));
        });

        List<RetrievalResult> results = hybridRetriever.hybridRetrieve("测试问题");

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("关键词检索异常完成 → 按空结果降级，向量侧照常融合（1.7 修正穿透）")
    void keywordExceptionDegradesToVectorOnly() {
        when(keywordRetriever.retrieve(anyString()))
                .thenThrow(new RuntimeException("ES 连接失败"));
        when(vectorRetriever.retrieve(anyString())).thenReturn(List.of(chunk("v1")));

        List<RetrievalResult> results = hybridRetriever.hybridRetrieve("测试问题");

        List<String> ids = results.stream().map(RetrievalResult::getChunkId).collect(Collectors.toList());
        assertThat(ids).contains("v1").doesNotContain("k1");
    }

    @Test
    @DisplayName("向量检索异常完成 → 按空结果降级，关键词侧照常融合")
    void vectorExceptionDegradesToKeywordOnly() {
        when(keywordRetriever.retrieve(anyString())).thenReturn(List.of(chunk("k1")));
        when(vectorRetriever.retrieve(anyString()))
                .thenThrow(new RuntimeException("Qdrant 连接失败"));

        List<RetrievalResult> results = hybridRetriever.hybridRetrieve("测试问题");

        List<String> ids = results.stream().map(RetrievalResult::getChunkId).collect(Collectors.toList());
        assertThat(ids).contains("k1").doesNotContain("v1");
    }

    @Test
    @DisplayName("两侧正常返回 → 融合结果包含双侧 chunk")
    void bothOnTimeFuseBothSides() {
        when(keywordRetriever.retrieve(anyString())).thenReturn(List.of(chunk("k1")));
        when(vectorRetriever.retrieve(anyString())).thenReturn(List.of(chunk("v1")));

        List<RetrievalResult> results = hybridRetriever.hybridRetrieve("测试问题");

        List<String> ids = results.stream().map(RetrievalResult::getChunkId).collect(Collectors.toList());
        assertThat(ids).contains("k1", "v1");
    }
}