-- ============================================================
-- Flyway V2: 初始账号（登录用 邮箱 + 密码）
--   admin@campus.com / 123456  (role=1 管理员)
--   user@campus.com  / 123456  (role=0 普通用户)
-- 密码为 BCrypt 哈希；登录后请尽快修改密码。
-- ============================================================

INSERT INTO `user` (name, email, password, role) VALUES
('admin', 'admin@campus.com', '$2b$10$X.JQtw4f4kuFXmbCWuHZOOHsgL46rNCV3hXzDrAd5OSNTSXDZNmv2', 1),
('user', 'user@campus.com', '$2b$10$X.JQtw4f4kuFXmbCWuHZOOHsgL46rNCV3hXzDrAd5OSNTSXDZNmv2', 0);
