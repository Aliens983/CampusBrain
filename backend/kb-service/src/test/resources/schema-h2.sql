-- ============================================================
-- H2 Test Database Schema (MySQL-compatible subset)
-- ============================================================

-- Document table
CREATE TABLE IF NOT EXISTS document
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(500)  NOT NULL,
    file_type   VARCHAR(20)   NOT NULL,
    file_size   BIGINT        NOT NULL,
    file_path   VARCHAR(1000) NOT NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'UPLOADED',
    chunk_count INT           DEFAULT 0,
    metadata    TEXT,
    error_msg   TEXT,
    deleted     TINYINT       DEFAULT 0,
    owner_id    BIGINT        DEFAULT NULL,
    tenant_id   BIGINT        DEFAULT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Document chunk table
CREATE TABLE IF NOT EXISTS document_chunk
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT        NOT NULL,
    chunk_index INT           NOT NULL,
    content     CLOB          NOT NULL,
    chunk_hash  VARCHAR(64)   NOT NULL,
    token_count INT           DEFAULT 0,
    metadata    TEXT,
    qdrant_id   VARCHAR(100),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0,
    CONSTRAINT fk_chunk_document FOREIGN KEY (document_id)
        REFERENCES document (id) ON DELETE CASCADE
);

-- Conversation table
CREATE TABLE IF NOT EXISTS conversation
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id      VARCHAR(64)  NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    content         CLOB         NOT NULL,
    references_json TEXT,
    feedback        VARCHAR(10),
    deleted         TINYINT      DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- (security 表 kb_user / oauth2_access_token / oauth2_refresh_token 已随自带登录体系下线 2026-09-07)
