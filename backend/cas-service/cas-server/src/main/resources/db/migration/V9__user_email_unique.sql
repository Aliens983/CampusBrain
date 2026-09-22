-- ---------------------------------------------------------------------------
-- V9 user.email 唯一约束
--
-- 背景：注册链路是「先查邮箱再插入」的两步操作，并发注册可插入同邮箱多账号；
--      V3 种子脚本的 INSERT IGNORE 也一直以 email 唯一索引为前提（此前实际缺失）。
--      重复账号会导致登录/取 id 随机命中、按邮箱改密一次波及多行、外键归属漂移。
--
-- 策略：先把同邮箱重复账号的数据并入最早保留的账号（id 最小），再删多余账号，
--      最后把普通索引升级为唯一索引。
-- ---------------------------------------------------------------------------

-- 1) 预约单并入保留账号
UPDATE item i
    JOIN `user` dup ON dup.id = i.user_id
    JOIN (
        SELECT email, MIN(id) AS keep_id
        FROM `user`
        GROUP BY email
        HAVING COUNT(*) > 1
    ) m ON m.email = dup.email AND dup.id <> m.keep_id
SET i.user_id = m.keep_id;

-- 2) 咨询师绑定并入保留账号
UPDATE consultant c
    JOIN `user` dup ON dup.id = c.user_id
    JOIN (
        SELECT email, MIN(id) AS keep_id
        FROM `user`
        GROUP BY email
        HAVING COUNT(*) > 1
    ) m ON m.email = dup.email AND dup.id <> m.keep_id
SET c.user_id = m.keep_id;

-- 3) 咨询沟通（代码级外键）：会话与消息发送者并入保留账号
UPDATE consult_chat_conversation conv
    JOIN `user` dup ON dup.id = conv.student_id
    JOIN (
        SELECT email, MIN(id) AS keep_id
        FROM `user`
        GROUP BY email
        HAVING COUNT(*) > 1
    ) m ON m.email = dup.email AND dup.id <> m.keep_id
SET conv.student_id = m.keep_id;

UPDATE consult_chat_conversation conv
    JOIN `user` dup ON dup.id = conv.teacher_id
    JOIN (
        SELECT email, MIN(id) AS keep_id
        FROM `user`
        GROUP BY email
        HAVING COUNT(*) > 1
    ) m ON m.email = dup.email AND dup.id <> m.keep_id
SET conv.teacher_id = m.keep_id;

UPDATE consult_chat_message msg
    JOIN `user` dup ON dup.id = msg.sender_id
    JOIN (
        SELECT email, MIN(id) AS keep_id
        FROM `user`
        GROUP BY email
        HAVING COUNT(*) > 1
    ) m ON m.email = dup.email AND dup.id <> m.keep_id
SET msg.sender_id = m.keep_id;

-- 4) 删除多余账号（预约单为 DB 外键 ON DELETE CASCADE，已先重绑，这里仅清理残留）
DELETE u
FROM `user` u
    JOIN (
        SELECT email, MIN(id) AS keep_id
        FROM `user`
        GROUP BY email
        HAVING COUNT(*) > 1
    ) m ON m.email = u.email AND u.id <> m.keep_id;

-- 5) 普通索引升级为唯一索引
ALTER TABLE `user` DROP INDEX idx_user_email, ADD UNIQUE KEY uk_user_email (email);
