-- 咨询师表
CREATE TABLE IF NOT EXISTS consultant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '咨询师ID',
    name VARCHAR(50) NOT NULL COMMENT '咨询师姓名',
    department VARCHAR(100) NOT NULL COMMENT '所属部门',
    title VARCHAR(50) NOT NULL COMMENT '职称',
    description VARCHAR(500) DEFAULT '' COMMENT '咨询师简介',
    rating DECIMAL(2,1) DEFAULT 5.0 COMMENT '评分',
    review_count INT DEFAULT 0 COMMENT '评价数量',
    avatar_url VARCHAR(255) DEFAULT '' COMMENT '头像URL',
    service_id BIGINT NOT NULL COMMENT '关联服务ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_service_id (service_id),
    CONSTRAINT fk_consultant_service FOREIGN KEY (service_id) REFERENCES services(service_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咨询师信息表';

-- 设备表
CREATE TABLE IF NOT EXISTS equipment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '设备ID',
    name VARCHAR(100) NOT NULL COMMENT '设备名称',
    category VARCHAR(50) NOT NULL DEFAULT '其他设备' COMMENT '设备分类',
    description VARCHAR(500) DEFAULT '' COMMENT '设备描述',
    total_stock INT NOT NULL DEFAULT 0 COMMENT '总库存',
    available_stock INT NOT NULL DEFAULT 0 COMMENT '可用库存',
    unit VARCHAR(20) NOT NULL DEFAULT '台' COMMENT '单位',
    location VARCHAR(200) NOT NULL DEFAULT '' COMMENT '存放位置',
    image_url VARCHAR(255) DEFAULT '' COMMENT '设备图片URL',
    service_id BIGINT NOT NULL COMMENT '关联服务ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_service_id (service_id),
    INDEX idx_category (category),
    CONSTRAINT fk_equipment_service FOREIGN KEY (service_id) REFERENCES services(service_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备信息表';

-- 咨询师数据
INSERT INTO consultant (name, department, title, description, rating, review_count, service_id) VALUES
('张老师', '学生咨询中心', '资深心理咨询师', '从事学生心理咨询工作10年，擅长学业压力、人际关系、情绪管理等领域', 4.8, 128, 2),
('李老师', '学生咨询中心', '高级职业规划师', '专注于大学生职业规划与就业指导，帮助学生明确职业方向', 4.9, 95, 2),
('王老师', '学生咨询中心', '心理咨询师', '擅长青少年心理辅导、学业规划、时间管理', 4.7, 86, 2),
('赵老师', '学业辅导中心', '高级学业导师', '擅长高等数学、线性代数等理工科课程的辅导', 4.6, 72, 3);

-- 设备数据（带分类）
INSERT INTO equipment (name, category, description, total_stock, available_stock, unit, location, service_id) VALUES
('投影仪', '投影设备', '高清投影仪，支持HDMI/VGA接口，适用于教学和会议', 10, 5, '台', '校园设备管理中心A区', 1),
('笔记本电脑', '计算机设备', 'ThinkPad T14，i7处理器，16GB内存，适合办公和编程', 20, 12, '台', '校园设备管理中心B区', 1),
('录音笔', '音频设备', '专业录音笔，支持远距离录音，适合课堂记录', 30, 22, '支', '校园设备管理中心C区', 1),
('摄像机', '摄影摄像', 'SONY 4K摄像机，适用于活动拍摄和课程录制', 8, 3, '台', '校园设备管理中心A区', 1);

-- 咨询可预约时段表
CREATE TABLE IF NOT EXISTS time_slot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '时段ID',
    consultant_id BIGINT NOT NULL COMMENT '咨询师ID',
    slot_date DATE NOT NULL COMMENT '日期',
    start_time VARCHAR(5) NOT NULL COMMENT '开始时间 HH:mm',
    end_time VARCHAR(5) NOT NULL COMMENT '结束时间 HH:mm',
    available TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可预约',
    INDEX idx_consultant_date (consultant_id, slot_date),
    CONSTRAINT fk_time_slot_consultant FOREIGN KEY (consultant_id) REFERENCES consultant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咨询可预约时段表';

-- 咨询时段种子数据（今天，每个咨询师若干时段）
INSERT INTO time_slot (consultant_id, slot_date, start_time, end_time, available) VALUES
(1, CURDATE(), '09:00', '10:00', 1),
(1, CURDATE(), '10:00', '11:00', 1),
(1, CURDATE(), '11:00', '12:00', 1),
(1, CURDATE(), '14:00', '15:00', 1),
(1, CURDATE(), '15:00', '16:00', 1),
(1, CURDATE(), '16:00', '17:00', 0),
(2, CURDATE(), '09:00', '10:00', 1),
(2, CURDATE(), '10:00', '11:00', 1),
(2, CURDATE(), '14:00', '15:00', 1),
(2, CURDATE(), '15:00', '16:00', 1);
