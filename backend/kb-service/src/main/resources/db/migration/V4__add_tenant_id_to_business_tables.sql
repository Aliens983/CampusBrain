-- 补充多租户 tenant_id 列（幂等：列已存在则跳过，兼容已手动迁移的库）

-- document 表
SET @doc_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'document' AND COLUMN_NAME = 'tenant_id'
);
SET @doc_sql = IF(@doc_exists = 0,
    'ALTER TABLE document ADD COLUMN tenant_id BIGINT NULL COMMENT ''租户ID'' AFTER owner_id',
    'SELECT 1');
PREPARE doc_stmt FROM @doc_sql;
EXECUTE doc_stmt;
DEALLOCATE PREPARE doc_stmt;

-- conversation 表
SET @conv_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'conversation' AND COLUMN_NAME = 'tenant_id'
);
SET @conv_sql = IF(@conv_exists = 0,
    'ALTER TABLE conversation ADD COLUMN tenant_id BIGINT NULL COMMENT ''租户ID'' AFTER id',
    'SELECT 1');
PREPARE conv_stmt FROM @conv_sql;
EXECUTE conv_stmt;
DEALLOCATE PREPARE conv_stmt;
