package com.kb.domain.document;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link DocumentDomainService}.
 * @author forever-king
 */
@DisplayName("DocumentDomainService 单元测试")
class DocumentDomainServiceTest {

    private final DocumentDomainService service = new DocumentDomainService();

    @Nested
    @DisplayName("文件类型检测")
    class FileTypeDetection {

        @Test
        @DisplayName("支持 PDF 格式")
        void shouldSupportPdf() {
            assertThat(service.isSupportedFileType("pdf")).isTrue();
            assertThat(service.isSupportedFileType("PDF")).isTrue();
        }

        @Test
        @DisplayName("支持 Markdown 格式")
        void shouldSupportMarkdown() {
            assertThat(service.isSupportedFileType("md")).isTrue();
            assertThat(service.isSupportedFileType("markdown")).isTrue();
        }

        @Test
        @DisplayName("支持 TXT 格式")
        void shouldSupportTxt() {
            assertThat(service.isSupportedFileType("txt")).isTrue();
        }

        @Test
        @DisplayName("支持 DOCX 格式")
        void shouldSupportDocx() {
            assertThat(service.isSupportedFileType("docx")).isTrue();
        }

        @Test
        @DisplayName("不支持 .exe 格式")
        void shouldRejectExe() {
            assertThat(service.isSupportedFileType("exe")).isFalse();
        }

        @Test
        @DisplayName("null 应返回 false")
        void shouldReturnFalseForNull() {
            assertThat(service.isSupportedFileType(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("文件扩展名提取")
    class FileExtension {

        @Test
        @DisplayName("应从文件名中正确提取扩展名")
        void shouldExtractExtension() {
            assertThat(service.extractFileType("document.pdf")).isEqualTo("pdf");
            assertThat(service.extractFileType("README.md")).isEqualTo("md");
            assertThat(service.extractFileType("notes.MD")).isEqualTo("md");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"noext", "file."})
        @DisplayName("无扩展名应返回 unknown")
        void shouldReturnUnknownForNoExtension(String filename) {
            assertThat(service.extractFileType(filename)).isEqualTo("unknown");
        }
    }

    @Nested
    @DisplayName("分块校验")
    class ChunkValidation {

        @Test
        @DisplayName("正常分块列表应校验通过")
        void shouldValidateCorrectChunks() {
            List<DocumentChunk> chunks = List.of(
                    DocumentChunk.builder().chunkIndex(0).content("a").build(),
                    DocumentChunk.builder().chunkIndex(1).content("b").build(),
                    DocumentChunk.builder().chunkIndex(2).content("c").build()
            );
            assertThatCode(() -> service.validateChunks(chunks))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("空列表应抛异常")
        void shouldThrowOnEmptyList() {
            assertThatThrownBy(() -> service.validateChunks(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null 应抛异常")
        void shouldThrowOnNull() {
            assertThatThrownBy(() -> service.validateChunks(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("索引不连续应抛异常")
        void shouldThrowOnIndexMismatch() {
            List<DocumentChunk> chunks = List.of(
                    DocumentChunk.builder().chunkIndex(1).content("skip-0").build()
            );
            assertThatThrownBy(() -> service.validateChunks(chunks))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
