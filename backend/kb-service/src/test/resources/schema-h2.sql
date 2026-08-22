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

-- User table (for security/authentication)
CREATE TABLE IF NOT EXISTS kb_user
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(50)   NOT NULL UNIQUE,
    email         VARCHAR(100)  NOT NULL UNIQUE,
    password_hash VARCHAR(255)  NOT NULL,
    role          VARCHAR(20)   NOT NULL DEFAULT 'USER',
    nickname      VARCHAR(100),
    avatar_url    VARCHAR(500),
    enabled       TINYINT       NOT NULL DEFAULT 1,
    deleted       TINYINT       DEFAULT 0,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- OAuth2 access token table
CREATE TABLE IF NOT EXISTS oauth2_access_token
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    access_token  VARCHAR(255) NOT NULL UNIQUE,
    refresh_token VARCHAR(255) NOT NULL,
    user_id       BIGINT       NOT NULL,
    username      VARCHAR(50),
    role          VARCHAR(20),
    nickname      VARCHAR(100),
    expires_time  TIMESTAMP    NOT NULL,
    deleted       TINYINT      DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- OAuth2 refresh token table
CREATE TABLE IF NOT EXISTS oauth2_refresh_token
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    refresh_token VARCHAR(255) NOT NULL UNIQUE,
    user_id       BIGINT       NOT NULL,
    deleted       TINYINT      DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
