package com.kb.domain.document;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link Document} aggregate root — state transitions.
 * @author forever-king
 */
@DisplayName("Document 聚合根状态机测试")
class DocumentTest {

    private Document uploadedDoc() {
        return Document.builder()
                .id(1L).title("测试文档").fileType("pdf")
                .status(DocumentStatus.UPLOADED).ownerId(1L)
                .build();
    }

    @Nested
    @DisplayName("状态流转")
    class StateTransitions {

        @Test
        @DisplayName("UPLOADED → PARSING 应正常")
        void shouldTransitionToParsing() {
            Document doc = uploadedDoc();
            doc.startParsing();
            assertThat(doc.getStatus()).isEqualTo(DocumentStatus.PARSING);
        }

        @Test
        @DisplayName("非 UPLOADED 状态调用 startParsing 应抛异常")
        void shouldThrowWhenParsingFromWrongState() {
            Document doc = Document.builder()
                    .status(DocumentStatus.READY).build();
            assertThatThrownBy(doc::startParsing)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("PARSING → CHUNKING 应正常")
        void shouldTransitionToChunking() {
            Document doc = uploadedDoc();
            doc.startParsing();
            doc.startChunking();
            assertThat(doc.getStatus()).isEqualTo(DocumentStatus.CHUNKING);
        }

        @Test
        @DisplayName("CHUNKING → EMBEDDING 应正常")
        void shouldTransitionToEmbedding() {
            Document doc = uploadedDoc();
            doc.startParsing();
            doc.startChunking();
            doc.startEmbedding();
            assertThat(doc.getStatus()).isEqualTo(DocumentStatus.EMBEDDING);
        }

        @Test
        @DisplayName("EMBEDDING → READY 应记录分块数")
        void shouldMarkReadyWithChunkCount() {
            Document doc = uploadedDoc();
            doc.startParsing();
            doc.startChunking();
            doc.startEmbedding();
            doc.markReady(42);

            assertThat(doc.getStatus()).isEqualTo(DocumentStatus.READY);
            assertThat(doc.getChunkCount()).isEqualTo(42);
            assertThat(doc.isReady()).isTrue();
        }

        @Test
        @DisplayName("markFailed 应设置错误信息")
        void shouldSetErrorOnFailure() {
            Document doc = uploadedDoc();
            doc.markFailed("解析失败：文件已损坏");

            assertThat(doc.getStatus()).isEqualTo(DocumentStatus.FAILED);
            assertThat(doc.getErrorMsg()).isEqualTo("解析失败：文件已损坏");
            assertThat(doc.isReady()).isFalse();
        }
    }

    @Nested
    @DisplayName("元数据操作")
    class Metadata {

        @Test
        @DisplayName("addMetadata 应正确添加键值对")
        void shouldAddMetadata() {
            Document doc = uploadedDoc();
            doc.addMetadata("author", "张三");
            doc.addMetadata("pages", 10);

            assertThat(doc.getMetadata()).containsEntry("author", "张三");
            assertThat(doc.getMetadata()).containsEntry("pages", 10);
        }

        @Test
        @DisplayName("metadata 初始为 null 时 addMetadata 应自动初始化")
        void shouldAutoInitMetadata() {
            Document doc = uploadedDoc();
            assertThat(doc.getMetadata()).isNull();

            doc.addMetadata("key", "value");
            assertThat(doc.getMetadata()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Builder 模式")
    class Builder {

        @Test
        @DisplayName("Builder 应正确构建所有字段")
        void shouldBuildAllFields() {
            Document doc = Document.builder()
                    .id(1L).title("手册").fileType("md")
                    .fileSize(1024L).filePath("/tmp/test.md")
                    .status(DocumentStatus.UPLOADED)
                    .chunkCount(0).ownerId(1L)
                    .build();

            assertThat(doc.getId()).isEqualTo(1L);
            assertThat(doc.getTitle()).isEqualTo("手册");
            assertThat(doc.getFileType()).isEqualTo("md");
            assertThat(doc.getFileSize()).isEqualTo(1024L);
        }
    }
}
