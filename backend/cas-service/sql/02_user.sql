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
    role     int default 0    not null comment 'Your role in this system.1 is admin,0 is user.'
)
collate=utf8mb4_unicode_ci;

create index email
    on cas_db.user (email);