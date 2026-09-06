-- ---------------------------------------------------------------------------
-- V5 咨询沟通（学生 ⇄ 教师 1:1 在线留言）
-- 仅新增两张新表，不改动任何既有表结构（遵循迁移约定：V*.sql 只面向全新机器做增量）。
-- 既有服务器库直接跑本文件即可（新迁移文件会被 Flyway 自动应用，无 checksum 冲突）。
-- 会话/消息均使用“代码级外键”（不建 DB 外键，与 services.category_id 的约定一致）：
--   conversation.student_id / teacher_id   → user.id
--   message.sender_id                      → user.id（学生或教师任一）
-- 业务口径：一条会话 = 一位学生 + 一位教师（唯一对），只服务于「教师咨询」场景，
-- 设备借用/教室空间/活动报名不开放聊天。
-- ---------------------------------------------------------------------------

-- 咨询会话表：一个学生与一位咨询教师之间的唯一一条持续会话
CREATE TABLE IF NOT EXISTS consult_chat_conversation
(
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    student_id BIGINT   NOT NULL COMMENT '学生用户ID（user.id，代码级外键）',
    teacher_id BIGINT   NOT NULL COMMENT '咨询教师用户ID（user.id，代码级外键，须为某 teacher 分类咨询师绑定账号）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cc_student_teacher (student_id, teacher_id),
    KEY idx_cc_teacher (teacher_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '咨询沟通会话表（学生-教师 1:1，一对唯一）';

-- 咨询聊天消息表
CREATE TABLE IF NOT EXISTS consult_chat_message
(
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    conversation_id BIGINT      NOT NULL COMMENT '会话ID（代码级外键 → consult_chat_conversation.id）',
    sender_id       BIGINT      NOT NULL COMMENT '发送者用户ID（学生或教师，代码级外键 → user.id）',
    content         TEXT        NOT NULL COMMENT '消息内容',
    read_flag       TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '对方是否已读（1已读 0未读，轮询拉取/打开会话时置1）',
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (id),
    KEY idx_ccm_conv (conversation_id, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '咨询沟通消息表';
