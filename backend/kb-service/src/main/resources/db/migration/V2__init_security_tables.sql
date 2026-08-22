-- ============================================================
-- Flyway Migration V2: 安全认证表（用户、双 Token）
-- 来源：sql/init-security.sql
-- ============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS kb_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) COMMENT '邮箱',
    password_hash VARCHAR(200) NOT NULL COMMENT 'BCrypt 加密密码',
    nickname VARCHAR(50) COMMENT '昵称',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN / USER',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Access Token 表（短有效期，UUID 令牌）
CREATE TABLE IF NOT EXISTS oauth2_access_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    access_token VARCHAR(64) NOT NULL UNIQUE COMMENT 'UUID 访问令牌',
    refresh_token VARCHAR(64) NOT NULL COMMENT '关联的刷新令牌',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role VARCHAR(20) NOT NULL COMMENT '用户角色',
    nickname VARCHAR(50) COMMENT '预加载的用户昵称',
    expires_time DATETIME NOT NULL COMMENT '过期时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_access_token (access_token),
    INDEX idx_refresh_token (refresh_token),
    INDEX idx_expires (expires_time),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Refresh Token 表（长有效期，UUID 令牌）
CREATE TABLE IF NOT EXISTS oauth2_refresh_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    refresh_token VARCHAR(64) NOT NULL UNIQUE COMMENT 'UUID 刷新令牌',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role VARCHAR(20) NOT NULL COMMENT '用户角色',
    expires_time DATETIME NOT NULL COMMENT '过期时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_refresh_token (refresh_token),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 默认管理员（密码 123456 的 BCrypt 哈希）
INSERT IGNORE INTO kb_user (username, password_hash, nickname, role)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6n5u', '管理员', 'ADMIN');
