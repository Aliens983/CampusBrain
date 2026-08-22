-- ============================================================
-- Flyway Migration V1: 核心业务表（文档、分块、对话、评测）
-- 来源：sql/init.sql
-- ============================================================

-- Document table: stores uploaded document metadata
CREATE TABLE IF NOT EXISTS document
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(500)  NOT NULL COMMENT 'Document title',
    file_type   VARCHAR(20)   NOT NULL COMMENT 'File type: PDF/MD/DOCX/HTML',
    file_size   BIGINT        NOT NULL COMMENT 'File size in bytes',
    file_path   VARCHAR(1000) NOT NULL COMMENT 'MinIO object path',
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
    role            VARCHAR(20)  NOT NULL COMMENT 'Message role: user/assistant/system',
    content         LONGTEXT     NOT NULL COMMENT 'Message content',
    references_json JSON COMMENT 'Citation references in JSON format',
    feedback        VARCHAR(10) COMMENT 'User feedback: like/dislike',
    deleted         TINYINT      DEFAULT 0 COMMENT 'Logical delete flag',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at),
    INDEX idx_session_created (session_id, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='Conversation history (Q&A pairs)';

-- Evaluation dataset table: test cases for RAG evaluation
CREATE TABLE IF NOT EXISTS eval_test_case
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    query             TEXT        NOT NULL COMMENT 'Test query',
    expected_answer   TEXT        NOT NULL COMMENT 'Expected answer',
    relevant_doc_ids  JSON COMMENT 'List of relevant document IDs',
    relevant_chunk_ids JSON COMMENT 'List of relevant chunk IDs',
    category          VARCHAR(50) COMMENT 'Test category: factual/analytical/procedural',
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category (category)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='Evaluation test cases for RAG performance measurement';
