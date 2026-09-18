-- ============================================================
-- Flyway Migration V3: 外部索引删除失败对账表（A-04）
-- 背景：删除文档时 Qdrant/ES 的删除是 best-effort，外部存储短暂故障会
--       留下孤儿向量/索引记录，检索可能返回已删除内容。
--       本表持久化删除失败项，由定时补偿任务 IndexDeleteFailureReclaimer
--       周期性重试，直至 RESOLVED；超过重试上限置 GIVE_UP 并暴露指标告警。
-- ============================================================

CREATE TABLE IF NOT EXISTS index_delete_failure
(
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT       NOT NULL COMMENT '被删除文档 ID',
    target      VARCHAR(20)  NOT NULL COMMENT '外部索引目标：QDRANT / ELASTICSEARCH',
    fail_reason VARCHAR(1000) COMMENT '最近一次失败原因（异常摘要）',
    retry_count INT          NOT NULL DEFAULT 0 COMMENT '补偿任务已重试次数',
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
        COMMENT '对账状态：PENDING（待补偿）/ RESOLVED（已恢复）/ GIVE_UP（超过上限待人工）',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_idf_doc_target (document_id, target),
    INDEX idx_idf_status_retry (status, retry_count)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='外部索引删除失败对账（A-04）';
