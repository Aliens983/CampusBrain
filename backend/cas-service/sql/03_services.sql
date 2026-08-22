-- Campus Appointment System - Services Table
-- 创建服务表

USE cas_db;

DROP TABLE IF EXISTS `services`;
CREATE TABLE `services` (
  `service_id` INT NOT NULL AUTO_INCREMENT COMMENT 'Services ID',
  `service_name` VARCHAR(20) DEFAULT NULL COMMENT 'Services name',
  `service_describe` VARCHAR(100) DEFAULT NULL COMMENT 'Services description',
  `service_state` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Services status: 0-disabled, 1-enabled',
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
  `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record update time',
  PRIMARY KEY (`service_id`),
  KEY `idx_service_name` (`service_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Services table - stores appointment services information';