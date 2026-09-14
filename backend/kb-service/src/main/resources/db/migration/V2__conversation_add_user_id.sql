-- ============================================================
-- Flyway Migration V2: 会话归属（4.1.13）
--
-- 背景：V1 的 conversation 表只有 session_id，全链路无
-- 「会话属于谁」的概念。sessionId 由前端生成，一旦经浏览
-- 器历史/分享/截图/日志泄露，持有者即可读取完整对话、清空
-- 他人上下文或刷反馈。
--
-- 改动：
--   1. conversation 增加 user_id，首条消息写入时绑定；
--   2. 增加 (user_id, session_id) 复合索引支撑归属查询。
--
-- 存量数据：user_id 允许 NULL。历史消息没有可靠的归属来源，
--   - 查询/重置/反馈全部带 user_id 条件，无主数据对任何用户
--     都不可见（安全姿态，而非继续暴露）；
--   - 首个持该 sessionId 发起新对话的用户写入时，会把该
--     session 下的无主消息一次性认领（UPDATE ... IS NULL）；
--   - 运维可按需 `DELETE FROM conversation WHERE user_id IS NULL`
--     清理。
-- ============================================================

ALTER TABLE conversation
    ADD COLUMN user_id BIGINT NULL COMMENT '会话归属用户 ID（首条消息写入时绑定）' AFTER session_id,
    ADD INDEX idx_user_session (user_id, session_id);
