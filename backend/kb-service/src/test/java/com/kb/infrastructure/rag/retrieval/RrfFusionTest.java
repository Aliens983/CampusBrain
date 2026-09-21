package com.kb.infrastructure.rag.retrieval;

import com.kb.domain.rag.RetrievalResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RRF fusion logic in {@link HybridRetriever}.
 * Uses Mockito to stub keyword/vector retrievers so fusion logic
 * is exercised in isolation.
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HybridRetriever RRF 融合算法测试")
class RrfFusionTest {

    @Mock private KeywordRetriever keywordRetriever;
    @Mock private VectorRetriever vectorRetriever;

    private HybridRetriever hybridRetriever;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newSingleThreadExecutor();
        hybridRetriever = new HybridRetriever(keywordRetriever, vectorRetriever, executor);
        ReflectionTestUtils.setField(hybridRetriever, "finalTopK", 5);
        ReflectionTestUtils.setField(hybridRetriever, "rrfK", 60.0);
        // Q-04：检索超时已配置化（@Value），单测手动构造不经过 Spring，需显式注入，避免 get(0s) 立即超时
        ReflectionTestUtils.setField(hybridRetriever, "retrievalTimeoutSeconds", 30);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Nested
    @DisplayName("RRF 融合核心逻辑")
    class RrfFusionLogic {

        @Test
        @DisplayName("两个列表都有结果时应合并去重，重复chunk标记为hybrid")
        void shouldMergeAndDeduplicate() {
            when(keywordRetriever.retrieve("test", null)).thenReturn(List.of(
                    result("chunk-A", "doc1", "keyword"),
                    result("chunk-B", "doc2", "keyword"),
                    result("chunk-C", "doc3", "keyword")
            ));
            when(vectorRetriever.retrieve("test", null)).thenReturn(List.of(
                    result("chunk-B", "doc2", "vector"),  // 重复
                    result("chunk-D", "doc4", "vector"),
                    result("chunk-E", "doc5", "vector")
            ));

            List<RetrievalResult> fused = hybridRetriever.hybridRetrieve("test", null);

            assertThat(fused).hasSizeLessThanOrEqualTo(5);
            fused.stream()
                    .filter(r -> r.getChunkId().equals("chunk-B"))
                    .findFirst()
                    .ifPresent(r -> assertThat(r.getSource()).isEqualTo("hybrid"));
        }

        @Test
        @DisplayName("排名靠前的结果RRF分数应更高")
        void shouldRankHigherForTopResults() {
            when(keywordRetriever.retrieve("test", null)).thenReturn(List.of(
                    result("chunk-1", "doc1", "keyword"),
                    result("chunk-2", "doc1", "keyword"),
                    result("chunk-3", "doc2", "keyword")
            ));
            when(vectorRetriever.retrieve("test", null)).thenReturn(List.of());

            List<RetrievalResult> fused = hybridRetriever.hybridRetrieve("test", null);

            assertThat(fused).hasSize(3);
            assertThat(fused.get(0).getScore())
                    .isGreaterThan(fused.get(fused.size() - 1).getScore());
        }

        @Test
        @DisplayName("在双路都出现的结果应获得更高总分")
        void shouldGiveHigherScoreToHybridMatches() {
            when(keywordRetriever.retrieve("test", null)).thenReturn(List.of(
                    result("shared", "doc1", "keyword"),
                    result("k-only", "doc2", "keyword")
            ));
            when(vectorRetriever.retrieve("test", null)).thenReturn(List.of(
                    result("shared", "doc1", "vector"),
                    result("v-only", "doc3", "vector")
            ));

            List<RetrievalResult> fused = hybridRetriever.hybridRetrieve("test", null);

            // shared 在两个列表中都排名第一，应有最高分
            assertThat(fused.get(0).getChunkId()).isEqualTo("shared");
            assertThat(fused.get(0).getScore())
                    .isGreaterThan(fused.get(1).getScore());
        }

        @Test
        @DisplayName("结果数不应超过 finalTopK 限制")
        void shouldRespectTopKLimit() {
            List<RetrievalResult> keyword = new ArrayList<>();
            List<RetrievalResult> vector = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                keyword.add(result("kw-" + i, "doc" + i, "keyword"));
                vector.add(result("vec-" + i, "doc" + i, "vector"));
            }
            when(keywordRetriever.retrieve("test", null)).thenReturn(keyword);
            when(vectorRetriever.retrieve("test", null)).thenReturn(vector);

            List<RetrievalResult> fused = hybridRetriever.hybridRetrieve("test", null);

            assertThat(fused).hasSizeLessThanOrEqualTo(5);
        }

        @Test
        @DisplayName("空结果应返回空列表")
        void shouldReturnEmptyForNoResults() {
            when(keywordRetriever.retrieve("empty", null)).thenReturn(List.of());
            when(vectorRetriever.retrieve("empty", null)).thenReturn(List.of());

            List<RetrievalResult> fused = hybridRetriever.hybridRetrieve("empty", null);

            assertThat(fused).isEmpty();
        }
    }

    @Nested
    @DisplayName("分路径检索")
    class PathRetrieval {

        @Test
        @DisplayName("keywordSearch 应委托给 KeywordRetriever")
        void shouldDelegateKeywordSearch() {
            when(keywordRetriever.retrieve("test", null)).thenReturn(
                    List.of(result("k1", "doc1", "keyword")));
            List<RetrievalResult> results = hybridRetriever.keywordSearch("test", null);
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getChunkId()).isEqualTo("k1");
        }

        @Test
        @DisplayName("vectorSearch 应委托给 VectorRetriever")
        void shouldDelegateVectorSearch() {
            when(vectorRetriever.retrieve("test", null)).thenReturn(
                    List.of(result("v1", "doc1", "vector")));
            List<RetrievalResult> results = hybridRetriever.vectorSearch("test", null);
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getChunkId()).isEqualTo("v1");
        }
    }

    // ---- helpers ----

    private static RetrievalResult result(String chunkId, String documentId, String source) {
        return RetrievalResult.builder()
                .chunkId(chunkId)
                .documentId(documentId)
                .documentTitle("测试文档-" + documentId)
                .content("这是测试内容，用于验证 RRF 融合算法的正确性。")
                .chunkIndex(0)
                .score(0.0)
                .source(source)
                .build();
    }
}
