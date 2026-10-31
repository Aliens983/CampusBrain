-- ============================================================
-- V2: 文档可见范围
--   PUBLIC  ：所有登录用户可检索（管理员上传的校园知识默认值）
--   PRIVATE ：仅 owner 本人可检索/查看
-- 存量数据按 PUBLIC 处理：此前上传入口仅管理员，文档均为公共知识；
-- 若不提升为 PUBLIC，普通用户将永远检索不到任何文档.
-- ============================================================

ALTER TABLE document
    ADD COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'PUBLIC'
        COMMENT '可见范围：PUBLIC 全员可检索 / PRIVATE 仅 owner' AFTER owner_id;

CREATE INDEX idx_document_visibility ON document (visibility);
