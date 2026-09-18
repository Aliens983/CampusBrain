package com.kb.infrastructure.persistence.mysql;

import com.kb.domain.document.IndexDeleteFailure;
import com.kb.domain.document.IndexDeleteFailureRepository;
import com.kb.domain.document.IndexTarget;
import com.kb.infrastructure.persistence.mysql.dataobject.IndexDeleteFailureDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * {@link IndexDeleteFailureRepository} 的 MySQL 实现（A-04）。
 *
 * @author forever-king
 */
@Repository
@RequiredArgsConstructor
public class IndexDeleteFailureRepositoryImpl implements IndexDeleteFailureRepository {

    /** 失败原因列长度 V3 迁移为 1000，落库前截断，避免过长堆栈导致写入失败。 */
    private static final int MAX_REASON_LENGTH = 1000;

    private final IndexDeleteFailureMapper mapper;

    @Override
    public void recordOrIncrement(Long documentId, IndexTarget target, String failReason) {
        String reason = truncate(failReason);
        mapper.insertOrIncrement(documentId, target.name(), reason);
    }

    @Override
    public List<IndexDeleteFailure> findPending(int maxRetryCount, int limit) {
        return mapper.selectPending(maxRetryCount, limit).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void markResolved(Long id) {
        mapper.updateResolved(id);
    }

    @Override
    public void markGiveUp(Long id) {
        mapper.updateGiveUp(id);
    }

    private IndexDeleteFailure toDomain(IndexDeleteFailureDO doObj) {
        IndexDeleteFailure domain = new IndexDeleteFailure(
                doObj.getDocumentId(),
                IndexTarget.valueOf(doObj.getTarget()),
                doObj.getFailReason(),
                doObj.getRetryCount() == null ? 0 : doObj.getRetryCount(),
                doObj.getStatus());
        domain.setId(doObj.getId());
        domain.setCreatedAt(doObj.getCreatedAt());
        domain.setUpdatedAt(doObj.getUpdatedAt());
        return domain;
    }

    private static String truncate(String reason) {
        if (reason == null) {
            return null;
        }
        return reason.length() <= MAX_REASON_LENGTH
                ? reason
                : reason.substring(0, MAX_REASON_LENGTH);
    }
}
