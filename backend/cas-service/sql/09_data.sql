-- Campus Appointment System - Sample Data
-- 初始化示例数据

USE cas_db;

-- 插入服务数据 (services表)
INSERT INTO `services` (`service_id`, `service_name`, `service_describe`, `service_state`) VALUES
(1, '空闲教室', '为学生提供空闲教室自习', 1),
(2, '心理咨询', '提供专业的心理咨询服务', 1),
(3, '学业辅导', '提供各学科的学业辅导服务', 1),
(5, '考试安排', '提供各类考试报名和安排服务', 1),
(6, '活动预约', '预约校园活动场地和资源', 1);
