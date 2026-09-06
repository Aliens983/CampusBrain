-- ============================================================
-- Flyway V1: CAS 初始 Schema + 参考/样例数据
-- 由 Flyway 应用启动时自动执行（不再手工跑 sql/ 脚本）
-- 对应旧 cas-service/sql 的 02~09 脚本，合并整理于此。
-- ============================================================

-- ---------- 用户表 ----------
CREATE TABLE `user`
(
    id           bigint auto_increment comment 'Primary key, auto-increment' primary key,
    name         varchar(64)      null comment 'User name',
    grade        varchar(32)      null comment 'Academic grade or class, optional',
    sex          varchar(8)       null comment 'Gender: male/female/other',
    age          tinyint unsigned null comment 'Age, unsigned (0-127)',
    email        varchar(128)     not null comment 'Email, must be unique if provided',
    password     varchar(255)     not null comment 'Hashed password (BCrypt), never store plain text',
    role         int default 0    not null comment 'Role: 0=USER,1=ADMIN,2=SUPER_ADMIN',
    email_notify tinyint(1) default 1 not null comment '是否接收邮件通知(0/1，通知设置)'
) collate = utf8mb4_unicode_ci comment = '用户表';

CREATE INDEX idx_user_email ON `user` (`email`);

-- ---------- 全局通知策略（单行 id=1） ----------
CREATE TABLE notification_policy
(
    id            tinyint  primary key comment '固定为1（单行策略）',
    email_enabled tinyint(1) not null default 1 comment '邮件通道是否启用',
    sms_enabled   tinyint(1) not null default 0 comment '短信通道是否启用',
    updated_at    datetime not null default current_timestamp on update current_timestamp comment '更新时间'
) collate = utf8mb4_unicode_ci comment = '全局通知策略';

INSERT INTO notification_policy (id) VALUES (1);

-- ---------- 服务表 ----------
CREATE TABLE services
(
    service_id      INT NOT NULL AUTO_INCREMENT comment 'Services ID',
    service_name    VARCHAR(20)  DEFAULT NULL comment 'Services name',
    service_describe VARCHAR(100) DEFAULT NULL comment 'Services description',
    service_state   TINYINT(1) NOT NULL DEFAULT 1 comment 'Services status: 0-disabled,1-enabled',
    category        VARCHAR(20) NOT NULL DEFAULT 'other' comment '业务分类: teacher/equipment/space/activity/exam/other',
    campus          VARCHAR(8)  NOT NULL DEFAULT 'cq' comment '校区: cq仓前 / xs下沙',
    image_url       VARCHAR(255) NOT NULL DEFAULT '' comment '服务封面图URL',
    capacity        INT NOT NULL DEFAULT -1 comment '可预约容量，-1=不限',
    booked_count    INT NOT NULL DEFAULT 0 comment '已预约数（乐观锁扣减）',
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP comment 'Record creation time',
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment 'Record update time',
    PRIMARY KEY (service_id),
    KEY idx_service_name (service_name),
    KEY idx_services_state (service_state)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci comment = 'Services table - stores appointment services';

-- ---------- 预约记录表 ----------
CREATE TABLE item
(
    order_id      INT NOT NULL AUTO_INCREMENT comment 'Order ID',
    user_id       BIGINT NOT NULL comment 'User ID, foreign key to user.id',
    service_id    INT NOT NULL comment 'Services ID, foreign key to services.service_id',
    consultant_id BIGINT      NULL comment '咨询师ID（咨询时段预约时非空）',
    slot_id       BIGINT      NULL comment 'time_slot 时段ID（咨询时段预约时非空）',
    slot_date     DATE        NULL comment '预约日期（咨询时段预约时非空）',
    start_time    VARCHAR(5)  NULL comment '时段开始 HH:mm',
    end_time      VARCHAR(5)  NULL comment '时段结束 HH:mm',
    equipment_id  BIGINT      NULL comment '设备ID（设备借用时非空）',
    quantity      INT         NULL DEFAULT 1 comment '借用数量',
    room_id       BIGINT      NULL comment '教室ID（教室时段预约时非空）',
    manage_status INT NOT NULL DEFAULT 0 comment 'Manage status: 0-pending,1-pass,2-reject,3-cancelled,4-completed',
    reason        VARCHAR(255) DEFAULT NULL comment 'Reject reason when audit is rejected',
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP comment 'Order creation time',
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment 'Order update time',
    PRIMARY KEY (order_id),
    KEY idx_user_id (user_id),
    KEY idx_service_id (service_id),
    KEY idx_manage_status (manage_status),
    KEY idx_user_status (user_id, manage_status),
    KEY idx_item_consultant_slot (consultant_id, slot_date),
    KEY idx_item_room (room_id),
    CONSTRAINT fk_item_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_item_service FOREIGN KEY (service_id) REFERENCES services (service_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci comment = 'Order table - stores user appointment orders';

-- ---------- 文件信息表 ----------
CREATE TABLE file_info
(
    id          bigint auto_increment comment 'Primary key' primary key,
    file_name   varchar(255) not null comment 'Original file name',
    file_path   varchar(500) not null comment 'Stored file path',
    file_size   bigint       null comment 'File size in bytes',
    file_type   varchar(100) null comment 'MIME type of the file',
    file_ext    varchar(20)  null comment 'File extension',
    file_uuid   varchar(64)  not null comment 'Unique UUID for file access',
    upload_user bigint       null comment 'User ID who uploaded the file',
    create_time datetime     not null comment 'Upload timestamp',
    update_time datetime     not null comment 'Last update timestamp',
    is_deleted  int default 0 not null comment 'Soft delete flag: 0-normal,1-deleted',
    KEY idx_file_uuid (file_uuid),
    KEY idx_upload_user (upload_user),
    KEY idx_create_time (create_time),
    KEY idx_file_is_deleted (is_deleted)
) collate = utf8mb4_unicode_ci comment = '文件信息表';

-- ---------- AI 对话历史表 ----------
CREATE TABLE ai_chat_history
(
    id               BIGINT NOT NULL AUTO_INCREMENT comment '主键',
    user_id          BIGINT NOT NULL comment '用户ID',
    model            VARCHAR(64) NOT NULL comment '使用的模型名称',
    user_message     TEXT NOT NULL comment '用户问题',
    ai_response      TEXT NOT NULL comment 'AI回答',
    response_time_ms INT DEFAULT 0 comment '响应时间(毫秒)',
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP comment '创建时间',
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci comment = 'AI对话历史记录表';

-- ---------- 咨询师表 ----------
CREATE TABLE consultant
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY comment '咨询师ID',
    name         VARCHAR(50) NOT NULL comment '咨询师姓名',
    department   VARCHAR(100) NOT NULL comment '所属部门',
    title        VARCHAR(50) NOT NULL comment '职称',
    description  VARCHAR(500) DEFAULT '' comment '咨询师简介',
    rating       DECIMAL(2, 1) DEFAULT 5.0 comment '评分',
    review_count INT DEFAULT 0 comment '评价数量',
    avatar_url   VARCHAR(255) DEFAULT '' comment '头像URL',
    service_id   INT NOT NULL comment '关联服务ID',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP comment '创建时间',
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment '更新时间',
    INDEX idx_consultant_service (service_id),
    CONSTRAINT fk_consultant_service FOREIGN KEY (service_id) REFERENCES services (service_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci comment = '咨询师信息表';

-- ---------- 设备表 ----------
CREATE TABLE equipment
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY comment '设备ID',
    name            VARCHAR(100) NOT NULL comment '设备名称',
    category        VARCHAR(50) NOT NULL DEFAULT '其他设备' comment '设备分类',
    description     VARCHAR(500) DEFAULT '' comment '设备描述',
    total_stock     INT NOT NULL DEFAULT 0 comment '总库存',
    available_stock INT NOT NULL DEFAULT 0 comment '可用库存',
    unit            VARCHAR(20) NOT NULL DEFAULT '台' comment '单位',
    location        VARCHAR(200) NOT NULL DEFAULT '' comment '存放位置',
    image_url       VARCHAR(255) DEFAULT '' comment '设备图片URL',
    service_id      INT NOT NULL comment '关联服务ID',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP comment '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment '更新时间',
    INDEX idx_equipment_service (service_id),
    INDEX idx_equipment_category (category),
    CONSTRAINT fk_equipment_service FOREIGN KEY (service_id) REFERENCES services (service_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci comment = '设备信息表';

-- ---------- 咨询可预约时段表 ----------
CREATE TABLE time_slot
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY comment '时段ID',
    consultant_id  BIGINT NOT NULL comment '咨询师ID',
    slot_date      DATE NOT NULL comment '日期',
    start_time     VARCHAR(5) NOT NULL comment '开始时间 HH:mm',
    end_time       VARCHAR(5) NOT NULL comment '结束时间 HH:mm',
    available      TINYINT(1) NOT NULL DEFAULT 1 comment '是否可预约',
    INDEX idx_time_slot_consultant (consultant_id, slot_date),
    CONSTRAINT fk_time_slot_consultant FOREIGN KEY (consultant_id) REFERENCES consultant (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci comment = '咨询可预约时段表';

-- ============================================================
-- 样例/参考数据
-- ============================================================
INSERT INTO services (service_id, service_name, service_describe, service_state, capacity, category, campus) VALUES
(1, '空闲教室', '为学生提供空闲教室自习', 1, -1, 'space', 'cq'),
(2, '心理咨询', '提供专业的心理咨询服务', 1, -1, 'teacher', 'cq'),
(3, '学业辅导', '提供各学科的学业辅导服务', 1, -1, 'teacher', 'cq'),
(6, '活动预约', '预约校园活动场地和资源（活动时间由发布方发布）', 1, 60, 'activity', 'cq'),
(7, '设备借用', '借用校园公共设备，按时间段预约，到点自动归还', 1, -1, 'equipment', 'cq'),
(8, '空闲教室', '为学生提供空闲教室自习', 1, -1, 'space', 'xs'),
(9, '心理咨询', '提供专业的心理咨询服务', 1, -1, 'teacher', 'xs'),
(10, '学业辅导', '提供各学科的学业辅导服务', 1, -1, 'teacher', 'xs'),
(11, '活动预约', '预约校园活动场地和资源（活动时间由发布方发布）', 1, 60, 'activity', 'xs'),
(12, '设备借用', '借用校园公共设备，按时间段预约，到点自动归还', 1, -1, 'equipment', 'xs');

INSERT INTO consultant (id, name, department, title, description, rating, review_count, service_id) VALUES
(1, '肖老师', '心理咨询中心', '咨询师', '擅长学业压力与情绪管理', 4.8, 100, 2),
(2, '周老师', '心理咨询中心', '咨询师', '擅长人际关系与职业规划', 4.8, 90, 2),
(3, '刘老师', '心理咨询中心', '咨询师', '擅长焦虑疏导与睡眠问题', 4.7, 85, 2),
(4, '石老师', '学业辅导中心', '学业导师', '擅长高等数学与线性代数', 4.7, 80, 3),
(5, '管老师', '学业辅导中心', '学业导师', '擅长物理与工程类课程', 4.6, 70, 3),
(6, '姚老师', '心理咨询中心', '咨询师', '擅长情绪管理与压力疏导', 4.8, 90, 9),
(7, '裘老师', '心理咨询中心', '咨询师', '擅长人际关系咨询', 4.7, 80, 9),
(8, '孙老师', '心理咨询中心', '咨询师', '擅长学习动力与时间管理', 4.6, 75, 9),
(9, '管老师', '学业辅导中心', '学业导师', '擅长英语与论文写作', 4.6, 60, 10);

INSERT INTO equipment (name, category, description, total_stock, available_stock, unit, location, service_id) VALUES
('投影仪', '投影设备', '高清投影仪，支持HDMI/VGA接口，适用于教学和会议', 10, 5, '台', '仓前设备中心', 7),
('笔记本电脑', '计算机设备', 'ThinkPad T14，i7处理器，16GB内存，适合办公和编程', 20, 12, '台', '仓前设备中心', 7),
('录音笔', '音频设备', '专业录音笔，支持远距离录音，适合课堂记录', 30, 22, '支', '下沙设备中心', 12),
('摄像机', '摄影摄像', 'SONY 4K摄像机，适用于活动拍摄和课程录制', 8, 3, '台', '下沙设备中心', 12);

INSERT INTO time_slot (consultant_id, slot_date, start_time, end_time, available) VALUES
(1, CURDATE(), '09:00', '10:00', 1),
(1, CURDATE(), '10:00', '11:00', 1),
(1, CURDATE(), '14:00', '15:00', 1),
(2, CURDATE(), '09:00', '10:00', 1),
(2, CURDATE(), '15:00', '16:00', 1),
(6, CURDATE(), '09:00', '10:00', 1),
(6, CURDATE(), '10:00', '11:00', 1),
(6, CURDATE(), '14:00', '15:00', 1);

-- ---------- 教室表 ----------
CREATE TABLE room
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY comment '教室ID',
    name        VARCHAR(100) NOT NULL comment '教室名称',
    location    VARCHAR(200) NOT NULL DEFAULT '' comment '位置',
    seats       INT NOT NULL DEFAULT 0 comment '容纳人数',
    service_id  INT NOT NULL comment '所属服务ID（空闲教室）',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_room_service (service_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci comment = '教室表 - 一间教室同一时间段仅一人预约';

INSERT INTO room (name, location, seats, service_id) VALUES
('勤园7号楼', '仓前校区', 40, 1),
('勤园8号楼', '仓前校区', 50, 1),
('勤园6号楼', '仓前校区', 40, 1),
('勤园13号楼', '仓前校区', 60, 1),
('恕园13号楼', '仓前校区', 60, 1),
('A号楼', '下沙校区', 40, 8),
('B号楼', '下沙校区', 50, 8),
('C号楼', '下沙校区', 40, 8),
('D号楼', '下沙校区', 60, 8),
('E号楼', '下沙校区', 60, 8);
