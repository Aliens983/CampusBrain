-- Campus Appointment System - Item Table (Order Table)
-- 创建订单表

USE cas_db;

DROP TABLE IF EXISTS `item`;
CREATE TABLE `item` (
  `order_id` INT NOT NULL AUTO_INCREMENT COMMENT 'Order ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID, foreign key to user.id',
  `service_id` INT NOT NULL COMMENT 'Services ID, foreign key to services.service_id',
  `manage_status` INT NOT NULL DEFAULT 0 COMMENT 'Manage status: 0-normal, 1-pass, 2-reject, 3-cancelled',
  `reason` VARCHAR(255) DEFAULT NULL COMMENT 'Reject reason when audit is rejected',
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Order creation time',
  `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Order update time',
  PRIMARY KEY (`order_id`),
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_service_id` (`service_id`) USING BTREE,
  CONSTRAINT `fk_item_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_item_service` FOREIGN KEY (`service_id`) REFERENCES `services` (`service_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Order table - stores user appointment orders';

-- 如果是已存在的数据库，需要执行以下 ALTER TABLE 语句添加 reason 字段：
-- ALTER TABLE `item` ADD COLUMN `reason` VARCHAR(255) DEFAULT NULL COMMENT 'Reject reason when audit is rejected' AFTER `manage_status`;
