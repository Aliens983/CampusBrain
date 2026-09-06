-- ============================================================
-- Flyway V3: 教师账号（role=3）+ 咨询师 1:1 绑定
--   每位咨询师绑定一个教师登录账号（拼音邮箱），教师登录即该咨询师本人，
--   可审「自己名下咨询档期」的学生预约。
--   幂等：邮箱已存在则跳过插入（INSERT IGNORE 依赖 user.email 唯一索引）。
--   注意：consultant.user_id 列在 V1（新机器）已建；已有老库需先直接
--   ALTER TABLE consultant ADD COLUMN user_id ... 后再应用本迁移。
-- ============================================================

INSERT IGNORE INTO `user` (name, email, password, role) VALUES
('肖老师', 'xiao@campus.com',   '$2b$10$X.JQtw4f4kuFXmbCWuHZOOHsgL46rNCV3hXzDrAd5OSNTSXDZNmv2', 3),
('周老师', 'zhou@campus.com',   '$2b$10$X.JQtw4f4kuFXmbCWuHZOOHsgL46rNCV3hXzDrAd5OSNTSXDZNmv2', 3),
('刘老师', 'liu@campus.com',    '$2b$10$X.JQtw4f4kuFXmbCWuHZOOHsgL46rNCV3hXzDrAd5OSNTSXDZNmv2', 3),
('石老师', 'shi@campus.com',    '$2b$10$X.JQtw4f4kuFXmbCWuHZOOHsgL46rNCV3hXzDrAd5OSNTSXDZNmv2', 3),
('管老师', 'guan@campus.com',   '$2b$10$X.JQtw4f4kuFXmbCWuHZOOHsgL46rNCV3hXzDrAd5OSNTSXDZNmv2', 3),
('姚老师', 'yao@campus.com',    '$2b$10$X.JQtw4f4kuFXmbCWuHZOOHsgL46rNCV3hXzDrAd5OSNTSXDZNmv2', 3),
('裘老师', 'qiu@campus.com',    '$2b$10$X.JQtw4f4kuFXmbCWuHZOOHsgL46rNCV3hXzDrAd5OSNTSXDZNmv2', 3),
('孙老师', 'sun@campus.com',    '$2b$10$X.JQtw4f4kuFXmbCWuHZOOHsgL46rNCV3hXzDrAd5OSNTSXDZNmv2', 3),
('管老师', 'guanxs@campus.com', '$2b$10$X.JQtw4f4kuFXmbCWuHZOOHsgL46rNCV3hXzDrAd5OSNTSXDZNmv2', 3);

-- 用「姓名 + 所属服务」绑定（不依赖自增 id，旧库 id 漂移也能正确匹配）
-- 咨询师归属（仓前 svc2/3：心理/辅导；下沙 svc9/10：心理/辅导）
UPDATE consultant c SET c.user_id = (SELECT id FROM `user` WHERE email = 'xiao@campus.com'   LIMIT 1) WHERE c.name = '肖老师' AND c.service_id = 2;
UPDATE consultant c SET c.user_id = (SELECT id FROM `user` WHERE email = 'zhou@campus.com'   LIMIT 1) WHERE c.name = '周老师' AND c.service_id = 2;
UPDATE consultant c SET c.user_id = (SELECT id FROM `user` WHERE email = 'liu@campus.com'    LIMIT 1) WHERE c.name = '刘老师' AND c.service_id = 2;
UPDATE consultant c SET c.user_id = (SELECT id FROM `user` WHERE email = 'shi@campus.com'    LIMIT 1) WHERE c.name = '石老师' AND c.service_id = 3;
UPDATE consultant c SET c.user_id = (SELECT id FROM `user` WHERE email = 'guan@campus.com'   LIMIT 1) WHERE c.name = '管老师' AND c.service_id = 3;
UPDATE consultant c SET c.user_id = (SELECT id FROM `user` WHERE email = 'yao@campus.com'    LIMIT 1) WHERE c.name = '姚老师' AND c.service_id = 9;
UPDATE consultant c SET c.user_id = (SELECT id FROM `user` WHERE email = 'qiu@campus.com'    LIMIT 1) WHERE c.name = '裘老师' AND c.service_id = 9;
UPDATE consultant c SET c.user_id = (SELECT id FROM `user` WHERE email = 'sun@campus.com'    LIMIT 1) WHERE c.name = '孙老师' AND c.service_id = 9;
UPDATE consultant c SET c.user_id = (SELECT id FROM `user` WHERE email = 'guanxs@campus.com' LIMIT 1) WHERE c.name = '管老师' AND c.service_id = 10;
