create table cas_db.user
(
    id       bigint auto_increment comment 'Primary key, auto-increment'
        primary key,
    name     varchar(64)      null comment 'User name, cannot be null',
    grade    varchar(32)      null comment 'Academic grade or class, optional',
    sex      varchar(8)       null comment 'Gender: male/female/other',
    age      tinyint unsigned null comment 'Age, unsigned (0–127), optional',
    email    varchar(128)     not null comment 'Email, must be unique if provided',
    password varchar(255)     not null comment 'Hashed password (e.g., BCrypt), never store plain text!',
    role     int default 0    not null comment 'Your role in this system.1 is admin,0 is user.',
    email_notify tinyint(1) default 1 not null comment '是否接收邮件通知(0/1，通知设置)'
)
collate=utf8mb4_unicode_ci;

create index email
    on cas_db.user (email);

-- 全局通知策略（单行 id=1）：管理端 系统设置 → 通知策略
create table if not exists cas_db.notification_policy
(
    id            tinyint      primary key comment '固定为 1（单行策略）',
    email_enabled tinyint(1) not null default 1 comment '邮件通道是否启用',
    sms_enabled   tinyint(1) not null default 0 comment '短信通道是否启用',
    updated_at    datetime   not null default current_timestamp on update current_timestamp comment '更新时间'
)
collate = utf8mb4_unicode_ci comment = '全局通知策略';

insert into cas_db.notification_policy (id) values (1)
on duplicate key update id = id;