package com.kb.infrastructure.persistence.mysql;

import com.kb.domain.document.DocumentChunk;
import com.kb.domain.document.DocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * {@link DocumentChunkTransactionService} 单事务边界守护测试（A-03）。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("分块批量写入单事务测试")
class DocumentChunkTransactionServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Test
    @DisplayName("委托仓储批量写入，且方法声明 @Transactional（任何异常回滚整批）")
    void delegatesAndIsTransactional() throws NoSuchMethodException {
        DocumentChunkTransactionService service = new DocumentChunkTransactionService(documentRepository);
        List<DocumentChunk> chunks = List.of(
                DocumentChunk.builder().chunkIndex(0).content("a").tokenCount(1).qdrantId("qd-1").build(),
                DocumentChunk.builder().chunkIndex(1).content("b").tokenCount(1).qdrantId("qd-2").build());

        service.saveChunksAtomically(chunks, 42L);

        verify(documentRepository).saveChunks(chunks, 42L);
        Transactional tx = DocumentChunkTransactionService.class
                .getMethod("saveChunksAtomically", List.class, Long.class)
                .getAnnotation(Transactional.class);
        assertThat(tx).as("分块批量写入必须处于单个数据库事务内").isNotNull();
        assertThat(tx.rollbackFor()).contains(Exception.class);
    }
}
