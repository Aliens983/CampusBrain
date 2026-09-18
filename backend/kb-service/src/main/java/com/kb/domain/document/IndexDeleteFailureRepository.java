package com.kb.domain.document;

import java.util.List;

/**
 * 外部索引删除失败对账仓储端口（A-04）。
 *
 * @author forever-king
 */
public interface IndexDeleteFailureRepository {

    /**
     * 记录一次删除失败；同一 (documentId, target) 已存在 PENDING 记录时，
     * 更新失败原因并将 retry_count +1，避免失败记录无限增长。
     */
    void recordOrIncrement(Long documentId, IndexTarget target, String failReason);

    /**
     * 捞取待补偿记录（status=PENDING 且重试次数未超上限），按最早更新时间优先。
     *
     * @param maxRetryCount 仅返回 retry_count &lt; maxRetryCount 的记录
     * @param limit         每轮最多条数
     */
    List<IndexDeleteFailure> findPending(int maxRetryCount, int limit);

    /**
     * 补偿成功：标记 RESOLVED。
     */
    void markResolved(Long id);

    /**
     * 达到重试上限：标记 GIVE_UP，等待人工核查（同时由指标告警暴露）。
     */
    void markGiveUp(Long id);
}
