package com.kb.infrastructure.rag.chunker;

import com.kb.domain.document.DocumentChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FixedSizeChunker}.
 * @author forever-king
 */
@DisplayName("FixedSizeChunker 单元测试")
class FixedSizeChunkerTest {

    private FixedSizeChunker chunker;

    @BeforeEach
    void setUp() {
        chunker = new FixedSizeChunker();
        ReflectionTestUtils.setField(chunker, "chunkSize", 512);
        ReflectionTestUtils.setField(chunker, "overlap", 50);
    }

    @Nested
    @DisplayName("基础分块功能")
    class BasicChunking {

        @Test
        @DisplayName("空文本应返回空列表")
        void shouldReturnEmptyForNullText() {
            List<DocumentChunk> chunks = chunker.chunk(null, null);
            assertThat(chunks).isEmpty();
        }

        @Test
        @DisplayName("空字符串应返回空列表")
        void shouldReturnEmptyForEmptyText() {
            List<DocumentChunk> chunks = chunker.chunk("", null);
            assertThat(chunks).isEmpty();
        }

        @Test
        @DisplayName("短文本（小于分块大小）应返回单个分块")
        void shouldReturnSingleChunkForShortText() {
            String text = "这是一段简短文本。";
            List<DocumentChunk> chunks = chunker.chunk(text, Map.of("docId", "test-1"));

            assertThat(chunks).hasSize(1);
            assertThat(chunks.get(0).getContent()).isEqualTo(text);
            assertThat(chunks.get(0).getChunkIndex()).isZero();
            assertThat(chunks.get(0).getMetadata()).containsEntry("docId", "test-1");
        }

        @Test
        @DisplayName("长文本应返回多个分块")
        void shouldReturnMultipleChunksForLongText() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                sb.append("这是第").append(i).append("段测试文本。这段文字用于验证分块功能是否正常工作。\n\n");
            }
            String text = sb.toString();

            List<DocumentChunk> chunks = chunker.chunk(text, null);
            assertThat(chunks).hasSizeGreaterThan(1);
        }
    }

    @Nested
    @DisplayName("分块属性")
    class ChunkProperties {

        @Test
        @DisplayName("每个分块应有递增的 chunkIndex")
        void shouldHaveIncrementalChunkIndices() {
            String text = "A".repeat(2000);
            List<DocumentChunk> chunks = chunker.chunk(text, null);

            for (int i = 0; i < chunks.size(); i++) {
                assertThat(chunks.get(i).getChunkIndex()).isEqualTo(i);
            }
        }

        @Test
        @DisplayName("每个分块的 tokenCount 应大于 0")
        void shouldHavePositiveTokenCount() {
            String text = "这是一个测试文档，包含足够的内容来生成有效的token计数。".repeat(20);
            List<DocumentChunk> chunks = chunker.chunk(text, null);

            for (DocumentChunk chunk : chunks) {
                assertThat(chunk.getTokenCount()).isPositive();
            }
        }

        @Test
        @DisplayName("metadata 应包含分块位置信息")
        void shouldIncludePositionMetadata() {
            String text = "测试内容。".repeat(100);
            List<DocumentChunk> chunks = chunker.chunk(text, null);

            for (DocumentChunk chunk : chunks) {
                assertThat(chunk.getMetadata()).containsKeys("chunkStart", "chunkEnd");
            }
        }
    }

    @Nested
    @DisplayName("自然断点检测")
    class NaturalBreakPoint {

        @Test
        @DisplayName("应优先在句号处断开")
        void shouldBreakAtPeriod() {
            chunker = new FixedSizeChunker();
            ReflectionTestUtils.setField(chunker, "chunkSize", 100);
            ReflectionTestUtils.setField(chunker, "overlap", 20);

            String text = "这是第一部分的内容，包含了一些信息。这是第二部分的内容，也包含了一些信息。";
            List<DocumentChunk> chunks = chunker.chunk(text, null);

            assertThat(chunks).isNotEmpty();
            // 分块应以句号结束
            for (DocumentChunk chunk : chunks) {
                String content = chunk.getContent();
                assertThat(content.endsWith("。") || content.equals(chunks.get(chunks.size() - 1).getContent()))
                        .as("Chunk should end with a period or be the last chunk")
                        .isTrue();
            }
        }
    }
}
