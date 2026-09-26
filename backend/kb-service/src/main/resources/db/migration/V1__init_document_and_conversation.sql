-- ============================================================
-- Flyway V1: KB 全量基线 Schema（文档、分块、对话、外部索引删除对账）
--
-- 开发期约定：当前无线上存量数据，所有表结构一律直接维护在本文件，
-- 不以 ALTER 增量脚本演进；确需改表就直接改这里（全新库重建生效）。
-- 将来上线、存在不可丢弃的存量数据后，再恢复「新增 Vn 只追加不改旧文件」规范。
-- ============================================================

-- Document table: stores uploaded document metadata
CREATE TABLE IF NOT EXISTS document
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(500)  NOT NULL COMMENT 'Document title',
    file_type   VARCHAR(20)   NOT NULL COMMENT 'File type: PDF/MD/DOCX/HTML',
    file_size   BIGINT        NOT NULL COMMENT 'File size in bytes',
    file_path   VARCHAR(1000) NOT NULL COMMENT 'Local file path (under app.file-storage-path)',
    status      VARCHAR(20)   NOT NULL DEFAULT 'UPLOADED'
        COMMENT 'Processing status: UPLOADED/PARSING/CHUNKING/EMBEDDING/READY/FAILED',
    chunk_count INT           DEFAULT 0 COMMENT 'Number of chunks',
    metadata    JSON COMMENT 'Custom metadata (author, pages, tags, etc.)',
    error_msg   TEXT COMMENT 'Error message if processing failed',
    deleted     TINYINT       DEFAULT 0 COMMENT 'Logical delete flag',
    owner_id    BIGINT COMMENT 'Document owner user ID',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_created (created_at),
    INDEX idx_file_type (file_type),
    INDEX idx_owner (owner_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='Uploaded document metadata';

-- Document chunk table: stores split chunks of each document
CREATE TABLE IF NOT EXISTS document_chunk
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT        NOT NULL COMMENT 'Parent document ID',
    chunk_index INT           NOT NULL COMMENT 'Chunk order index (0-based)',
    content     LONGTEXT      NOT NULL COMMENT 'Chunk text content',
    chunk_hash  VARCHAR(64)   NOT NULL COMMENT 'SHA-256 content hash for incremental update detection',
    token_count INT           DEFAULT 0 COMMENT 'Estimated token count',
    metadata    JSON COMMENT 'Chunk metadata (page_number, section_title, etc.)',
    qdrant_id   VARCHAR(100) COMMENT 'Corresponding point ID in Qdrant',
    deleted     TINYINT       DEFAULT 0 COMMENT 'Logical delete flag',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_document_id (document_id),
    INDEX idx_qdrant_id (qdrant_id),
    INDEX idx_chunk_hash (chunk_hash),
    CONSTRAINT fk_chunk_document FOREIGN KEY (document_id)
        REFERENCES document (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='Document chunks after splitting';

-- Conversation table: stores Q&A conversation history
CREATE TABLE IF NOT EXISTS conversation
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id      VARCHAR(64)  NOT NULL COMMENT 'Session identifier',
    user_id         BIGINT       NULL COMMENT '会话归属用户 ID（首条消息写入时绑定；无主数据对所有用户不可见）',
    role            VARCHAR(20)  NOT NULL COMMENT 'Message role: user/assistant/system',
    content         LONGTEXT     NOT NULL COMMENT 'Message content',
    references_json JSON COMMENT 'Citation references in JSON format',
    feedback        VARCHAR(10) COMMENT 'User feedback: like/dislike',
    deleted         TINYINT      DEFAULT 0 COMMENT 'Logical delete flag',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id),
    INDEX idx_user_session (user_id, session_id),
    INDEX idx_created_at (created_at),
    INDEX idx_session_created (session_id, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='Conversation history (Q&A pairs)';

-- 外部索引删除失败对账表：删除文档时 Qdrant/ES 的删除是 best-effort，
-- 外部存储短暂故障会留下孤儿向量/索引记录；本表持久化删除失败项，
-- 由定时补偿任务 IndexDeleteFailureReclaimer 周期重试直至 RESOLVED，
-- 超过重试上限置 GIVE_UP 并暴露指标告警。
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
