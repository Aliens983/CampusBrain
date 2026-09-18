package com.kb.infrastructure.persistence.mysql;

import com.kb.domain.document.DocumentChunk;
import com.kb.domain.document.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文档分块 MySQL 写入的事务边界（A-03 收尾）。
 * <p>
 * {@code DocumentRepository.saveChunks} 是逐行 INSERT，此前由 MQ 消费者直接调用，
 * 没有外层事务：写到第 N 行失败时前 N-1 行已提交，留下半成品分块（后续向量/ES 步骤
 * 失败后文档被置 FAILED，但部分分块残留，重投虽有 Step0 前缀清理仍可能与新写入交错）。
 * <p>
 * 将整批分块写入收敛到<strong>单个事务</strong>：任意一行失败整体回滚，
 * MySQL 侧要么全有要么全无；消费重试时面对的是干净状态。
 * 注意：Qdrant/ES 等外部存储不支持本地事务，仍由 Step0 幂等清理保证最终一致。
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentChunkTransactionService {

    private final DocumentRepository documentRepository;

    /**
     * 整批分块在单个事务内写入；任一行失败全部回滚。
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveChunksAtomically(List<DocumentChunk> chunks, Long documentId) {
        documentRepository.saveChunks(chunks, documentId);
    }
}
