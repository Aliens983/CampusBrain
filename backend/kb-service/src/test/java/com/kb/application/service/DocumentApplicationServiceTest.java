package com.kb.application.service;

import com.kb.domain.document.Document;
import com.kb.domain.document.DocumentRepository;
import com.kb.domain.rag.VectorStoreService;
import com.kb.infrastructure.metrics.BusinessMetrics;
import com.kb.infrastructure.mq.DocumentProcessingProducer;
import com.kb.infrastructure.persistence.elasticsearch.EsDocumentRepository;
import com.kb.infrastructure.rag.parser.DocumentParserSpi;
import com.kb.infrastructure.rag.parser.ParserRegistry;
import com.kb.infrastructure.security.LoginUser;
import com.kb.infrastructure.security.SecurityFrameworkUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 文档应用服务测试：管理员搜索下推、删除"先 DB 后外部存储"、上传失败补偿。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("文档生命周期一致性测试")
class DocumentApplicationServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentProcessingProducer mqProducer;
    @Mock private VectorStoreService vectorStore;
    @Mock private EsDocumentRepository esRepository;
    @Mock private BusinessMetrics metrics;
    @Mock private ParserRegistry parserRegistry;

    private DocumentApplicationService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new DocumentApplicationService(documentRepository, mqProducer, vectorStore,
                esRepository, metrics, parserRegistry);
        ReflectionTestUtils.setField(service, "fileStoragePath", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        SecurityFrameworkUtils.clearContext();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
    }

    private void login(Long userId, String role) {
        SecurityFrameworkUtils.setLoginUser(LoginUser.builder()
                .userId(userId).username("u" + userId).role(role).build());
    }

    @Test
    @DisplayName("管理员关键词搜索下推 SQL，不再 findAll 全表内存过滤")
    void adminSearchShouldPushDownToSql() {
        login(1L, "ADMIN");
        when(documentRepository.searchAllByTitle(eq("向量"), eq(0), eq(20)))
                .thenReturn(List.of());

        service.searchVisibleDocuments("向量", 0, 20);

        verify(documentRepository).searchAllByTitle("向量", 0, 20);
        verify(documentRepository, never()).findAll(anyInt(), anyInt());
    }

    @Test
    @DisplayName("普通用户搜索带 owner 条件且 size 上限被截断到 100")
    void userSearchShouldClampSize() {
        login(9L, "USER");
        when(documentRepository.searchByOwnerIdAndTitle(eq(9L), eq("rag"), eq(2), eq(100)))
                .thenReturn(List.of());

        service.searchVisibleDocuments("rag", 2, 999);

        verify(documentRepository).searchByOwnerIdAndTitle(9L, "rag", 2, 100);
    }

    @Test
    @DisplayName("删除：无事务环境下 DB 删除先于向量/ES/文件清理")
    void deleteShouldRemoveDbBeforeExternalStores() throws Exception {
        login(1L, "USER");
        Path file = tempDir.resolve("doc.txt");
        Files.writeString(file, "data");
        Document doc = Document.builder().id(5L).ownerId(1L).filePath(file.toString()).build();
        when(documentRepository.findById(5L)).thenReturn(Optional.of(doc));

        service.deleteDocument(5L);

        InOrder inOrder = inOrder(documentRepository, vectorStore, esRepository);
        inOrder.verify(documentRepository).delete(5L);
        inOrder.verify(vectorStore).deleteByDocumentId("5");
        inOrder.verify(esRepository).deleteByDocumentId("5");
        assertThat(Files.exists(file)).isFalse();
        verify(metrics, never()).recordDocumentFailure();
    }

    @Test
    @DisplayName("删除：有事务时外部存储清理延迟到 afterCommit，未提交前不动外部存储")
    void deleteShouldDeferExternalCleanupUntilCommit() {
        login(1L, "USER");
        Document doc = Document.builder().id(6L).ownerId(1L).filePath(null).build();
        when(documentRepository.findById(6L)).thenReturn(Optional.of(doc));
        TransactionSynchronizationManager.initSynchronization();

        service.deleteDocument(6L);

        verify(documentRepository).delete(6L);
        verifyNoInteractions(vectorStore, esRepository);

        // 模拟事务提交
        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        assertThat(syncs).hasSize(1);
        syncs.forEach(TransactionSynchronization::afterCommit);

        verify(vectorStore).deleteByDocumentId("6");
        verify(esRepository).deleteByDocumentId("6");
    }

    @Test
    @DisplayName("删除：事务回滚（afterCommit 未触发）时外部存储一律不动")
    void rollbackShouldSkipExternalCleanup() {
        login(1L, "USER");
        Document doc = Document.builder().id(8L).ownerId(1L).filePath(null).build();
        when(documentRepository.findById(8L)).thenReturn(Optional.of(doc));
        TransactionSynchronizationManager.initSynchronization();

        service.deleteDocument(8L);

        // 不触发 afterCommit，直接清理同步器（模拟回滚后的清理）
        TransactionSynchronizationManager.clear();

        verifyNoInteractions(vectorStore, esRepository);
    }

    @Test
    @DisplayName("删除：向量删除最终失败时仍清理 ES，且计入失败指标（不静默吞掉）")
    void cleanupFailureShouldStillProceedAndRecordMetric() {
        login(1L, "USER");
        Document doc = Document.builder().id(9L).ownerId(1L).filePath(null).build();
        when(documentRepository.findById(9L)).thenReturn(Optional.of(doc));
        doThrow(new RuntimeException("qdrant down"))
                .when(vectorStore).deleteByDocumentId("9");

        service.deleteDocument(9L);

        verify(vectorStore, times(3)).deleteByDocumentId("9");
        verify(esRepository).deleteByDocumentId("9");
        verify(metrics).recordDocumentFailure();
    }

    @Test
    @DisplayName("上传：DB 落库失败时补偿删除已落盘文件并向上抛出")
    void uploadFailureShouldCompensateLocalFile() throws Exception {
        login(1L, "USER");
        MockMultipartFile file = new MockMultipartFile(
                "file", "note.txt", "text/plain", "hello".getBytes());
        when(parserRegistry.getParserChain("txt")).thenReturn(List.of(mock(DocumentParserSpi.class)));
        when(documentRepository.save(any(Document.class)))
                .thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> service.uploadDocument(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("db down");

        assertThat(Files.list(tempDir).count()).isZero();
        verify(mqProducer, never()).send(anyLong());
    }

    @Test
    @DisplayName("上传：成功时 MQ 消息延迟到事务提交后发送")
    void uploadShouldSendMqAfterCommit() {
        login(1L, "USER");
        MockMultipartFile file = new MockMultipartFile(
                "file", "note2.txt", "text/plain", "world".getBytes());
        when(parserRegistry.getParserChain("txt")).thenReturn(List.of(mock(DocumentParserSpi.class)));
        when(documentRepository.save(any(Document.class)))
                .thenReturn(Document.builder().id(11L).build());
        TransactionSynchronizationManager.initSynchronization();

        Long id = service.uploadDocument(file);

        assertThat(id).isEqualTo(11L);
        verify(mqProducer, never()).send(anyLong());

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(mqProducer).send(11L);
        verify(metrics).recordDocumentUpload();
    }
}
