package com.kb.infrastructure.mq;

import com.kb.domain.document.Document;
import com.kb.domain.document.DocumentChunk;
import com.kb.domain.document.DocumentRepository;
import com.kb.domain.document.DocumentStatus;
import com.kb.domain.rag.EmbeddingService;
import com.kb.domain.rag.ParsedDocument;
import com.kb.domain.rag.VectorStoreService;
import com.kb.infrastructure.persistence.elasticsearch.EsDocumentEntity;
import com.kb.infrastructure.persistence.elasticsearch.EsDocumentRepository;
import com.kb.infrastructure.persistence.mysql.DocumentChunkTransactionService;
import com.kb.infrastructure.rag.chunker.ChunkerFactory;
import com.kb.infrastructure.rag.chunker.FixedSizeChunker;
import com.kb.infrastructure.rag.chunker.SlidingWindowChunker;
import com.kb.infrastructure.rag.graph.KnowledgeGraphService;
import com.kb.infrastructure.rag.parser.ParserChain;
import com.kb.infrastructure.rag.parser.ParserFactory;
import com.kb.infrastructure.schedule.DocumentProcessingLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文档处理管线的 documentTitle 注入测试.
 * <p>
 * 背景：解析器 metadata 只带 author/pageCount 等统计信息，从不写 documentTitle；
 * 而 ES/Qdrant 的 document_title、引用展示、RAG 提示词"来源"行都按该键读取——
 * 缺了它引用永远显示"未知文档".消费者必须在 chunk 阶段显式注入.
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("文档处理管线标题注入")
class DocumentProcessingConsumerTitleTest {

    @TempDir
    Path tempDir;

    @Mock private DocumentRepository documentRepository;
    @Mock private ParserFactory parserFactory;
    @Mock private ParserChain parserChain;
    @Mock private EmbeddingService embeddingService;
    @Mock private VectorStoreService vectorStore;
    @Mock private EsDocumentRepository esRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private KnowledgeGraphService kgService;
    @Mock private DocumentProcessingLock processingLock;
    @Mock private DocumentChunkTransactionService chunkTransactionService;

    private DocumentProcessingConsumer consumer;

    @BeforeEach
    void setUp() throws IOException {
        SlidingWindowChunker sliding = new SlidingWindowChunker();
        ReflectionTestUtils.setField(sliding, "windowSize", 512);
        ReflectionTestUtils.setField(sliding, "overlap", 50);
        FixedSizeChunker fixed = new FixedSizeChunker();
        ReflectionTestUtils.setField(fixed, "chunkSize", 512);
        ReflectionTestUtils.setField(fixed, "overlap", 50);
        ChunkerFactory chunkerFactory = new ChunkerFactory(fixed, sliding);

        consumer = new DocumentProcessingConsumer(documentRepository, parserFactory, parserChain,
                chunkerFactory, embeddingService, vectorStore, esRepository, eventPublisher,
                kgService, processingLock, chunkTransactionService);
        ReflectionTestUtils.setField(consumer, "chunkStrategy", "sliding_window");
    }

    @Test
    @DisplayName("ES/Qdrant 写入的 document_title 应为库中文档名（修复引用显示\"未知文档\"）")
    void shouldInjectDocumentTitleIntoChunkMetadata() throws IOException {
        Path file = Files.createFile(tempDir.resolve("guide.md"));
        Files.writeString(file, "校园卡可用于食堂消费与门禁。\n\n补卡点设在仓前校区服务大厅。");
        Document doc = Document.builder()
                .id(42L).title("校园卡使用指南").fileType("md")
                .filePath(file.toString()).status(DocumentStatus.PARSING).ownerId(7L)
                .build();
        when(documentRepository.findById(42L)).thenReturn(Optional.of(doc));
        when(processingLock.enterProcessing(42L)).thenReturn(true);
        Map<String, Object> parserMeta = new HashMap<>();
        parserMeta.put("author", "教务处");
        when(parserChain.parseAuto(any(), any())).thenReturn(ParsedDocument.builder()
                .title("解析器标题").content("校园卡可用于食堂消费与门禁。\n\n补卡点设在仓前校区服务大厅。")
                .fileType("md").metadata(parserMeta).build());
        when(embeddingService.embedBatch(anyList()))
                .thenAnswer(inv -> inv.<List<String>>getArgument(0).stream()
                        .map(s -> new float[4]).toList());

        consumer.processDocument(new DocumentProcessingMessage(42L, false));

        // ES 侧：document_title 必须是文档名，而非空串/解析器标题
        ArgumentCaptor<List<EsDocumentEntity>> esCaptor = ArgumentCaptor.forClass(List.class);
        verify(esRepository).bulkIndex(esCaptor.capture());
        assertFalse(esCaptor.getValue().isEmpty(), "应有分块写入 ES");
        for (EsDocumentEntity entity : esCaptor.getValue()) {
            assertEquals("校园卡使用指南", entity.getDocumentTitle(),
                    "ES document_title 不得为空：引用展示与检索都依赖它");
        }

        // Qdrant 侧：payload 的 document_title 同样注入
        ArgumentCaptor<List<VectorStoreService.VectorPoint>> qdrantCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).upsert(qdrantCaptor.capture());
        for (VectorStoreService.VectorPoint point : qdrantCaptor.getValue()) {
            assertEquals("校园卡使用指南", point.payload().get("document_title"));
        }
    }

    @Test
    @DisplayName("文档名为空时回退解析器标题，仍不得写入空串")
    void shouldFallbackToParsedTitleWhenDocumentTitleBlank() throws IOException {
        Path file = Files.createFile(tempDir.resolve("empty-title.md"));
        Files.writeString(file, "第一段内容。\n\n第二段内容。");
        Document doc = Document.builder()
                .id(43L).title("  ").fileType("md")
                .filePath(file.toString()).status(DocumentStatus.PARSING).ownerId(7L)
                .build();
        when(documentRepository.findById(43L)).thenReturn(Optional.of(doc));
        when(processingLock.enterProcessing(43L)).thenReturn(true);
        when(parserChain.parseAuto(any(), any())).thenReturn(ParsedDocument.builder()
                .title("解析器标题").content("第一段内容。\n\n第二段内容。")
                .fileType("md").build());
        when(embeddingService.embedBatch(anyList()))
                .thenAnswer(inv -> inv.<List<String>>getArgument(0).stream()
                        .map(s -> new float[4]).toList());

        consumer.processDocument(new DocumentProcessingMessage(43L, false));

        ArgumentCaptor<List<EsDocumentEntity>> esCaptor = ArgumentCaptor.forClass(List.class);
        verify(esRepository).bulkIndex(esCaptor.capture());
        for (EsDocumentEntity entity : esCaptor.getValue()) {
            assertEquals("解析器标题", entity.getDocumentTitle());
        }
    }
}
