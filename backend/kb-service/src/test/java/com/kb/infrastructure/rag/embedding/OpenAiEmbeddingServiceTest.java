package com.kb.infrastructure.rag.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * {@link OpenAiEmbeddingService} 分批并发向量化测试。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Embedding 分批并发测试")
class OpenAiEmbeddingServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    private OpenAiEmbeddingService service;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        service = new OpenAiEmbeddingService(embeddingModel);
        executor = Executors.newFixedThreadPool(4);
        ReflectionTestUtils.setField(service, "batchSize", 32);
        ReflectionTestUtils.setField(service, "embeddingExecutor", executor);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    /** 桩：按入参条数返回向量，向量首维编码文本长度以便校验对齐 */
    @SuppressWarnings("unchecked")
    private void stubEchoEmbeddings() {
        when(embeddingModel.embedAll(anyList())).thenAnswer(inv -> {
            List<TextSegment> segments = inv.getArgument(0);
            List<Embedding> embeddings = new ArrayList<>();
            for (TextSegment segment : segments) {
                embeddings.add(Embedding.from(new float[]{segment.text().length()}));
            }
            return Response.from(embeddings);
        });
    }

    @Test
    @DisplayName("65 条文本按 32 条切 3 批并发，结果与输入严格同序对齐")
    void shouldEmbedInParallelBatchesAndKeepOrder() {
        stubEchoEmbeddings();
        List<String> texts = new ArrayList<>();
        for (int i = 1; i <= 65; i++) {
            texts.add("x".repeat(i));
        }

        List<float[]> results = service.embedBatch(texts);

        assertThat(results).hasSize(65);
        for (int i = 0; i < 65; i++) {
            assertThat(results.get(i)[0])
                    .as("第 %d 条向量应与输入文本对齐", i)
                    .isEqualTo((float) (i + 1));
        }
        verify(embeddingModel, times(3)).embedAll(anyList());
    }

    @Test
    @DisplayName("空列表直接返回，不调用模型")
    void shouldReturnEmptyForEmptyInput() {
        assertThat(service.embedBatch(List.of())).isEmpty();
        verifyNoInteractions(embeddingModel);
    }

    @Test
    @DisplayName("不足一批时只发一次请求")
    void shouldSendSingleRequestForSmallInput() {
        stubEchoEmbeddings();
        List<float[]> results = service.embedBatch(List.of("a", "bb", "ccc"));

        assertThat(results).hasSize(3);
        assertThat(results.get(1)[0]).isEqualTo(2.0f);
        verify(embeddingModel, times(1)).embedAll(anyList());
    }

    @Test
    @DisplayName("某批模型调用失败时异常上抛，不静默写零向量")
    @SuppressWarnings("unchecked")
    void shouldPropagateBatchFailure() {
        AtomicInteger calls = new AtomicInteger();
        when(embeddingModel.embedAll(anyList())).thenAnswer(inv -> {
            if (calls.incrementAndGet() == 2) {
                throw new RuntimeException("rate limited");
            }
            List<TextSegment> segments = inv.getArgument(0);
            List<Embedding> embeddings = segments.stream()
                    .map(s -> Embedding.from(new float[]{s.text().length()}))
                    .toList();
            return Response.from(embeddings);
        });
        List<String> texts = new ArrayList<>();
        for (int i = 1; i <= 65; i++) {
            texts.add("x".repeat(i));
        }

        assertThatThrownBy(() -> service.embedBatch(texts))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Embedding 批处理失败");
    }

    @Test
    @DisplayName("模型返回条数少于请求条数时快速失败，避免下标错位")
    @SuppressWarnings("unchecked")
    void shouldFailOnSizeMismatch() {
        when(embeddingModel.embedAll(anyList())).thenAnswer(inv ->
                Response.from(List.of(Embedding.from(new float[]{1f}))));

        assertThatThrownBy(() -> service.embedBatch(List.of("a", "bb")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("返回条数不匹配");
    }
}
