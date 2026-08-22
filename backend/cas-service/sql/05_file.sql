create table cas_db.file_info
(
    id           bigint auto_increment comment 'Primary key, auto-increment'
        primary key,
    file_name    varchar(255)     not null comment 'Original file name',
    file_path    varchar(500)     not null comment 'Stored file path in local system',
    file_size    bigint          null comment 'File size in bytes',
    file_type    varchar(100)    null comment 'MIME type of the file',
    file_ext     varchar(20)     null comment 'File extension',
    file_uuid    varchar(64)     not null comment 'Unique UUID for file access',
    upload_user  bigint          null comment 'User ID who uploaded the file',
    create_time  datetime        not null comment 'Upload timestamp',
    update_time  datetime        not null comment 'Last update timestamp',
    is_deleted   int default 0   not null comment 'Soft delete flag: 0-normal, 1-deleted'
)
collate=utf8mb4_unicode_ci;

create index idx_file_uuid
    on cas_db.file_info (file_uuid);

create index idx_upload_user
    on cas_db.file_info (upload_user);

create index idx_create_time
    on cas_db.file_info (create_time);
