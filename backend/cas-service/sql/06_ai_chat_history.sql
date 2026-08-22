CREATE TABLE `ai_chat_history` (
                                   `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                   `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                   `model` VARCHAR(64) NOT NULL COMMENT '使用的模型名称',
                                   `user_message` TEXT NOT NULL COMMENT '用户问题',
                                   `ai_response` TEXT NOT NULL COMMENT 'AI回答',
                                   `response_time_ms` INT DEFAULT 0 COMMENT '响应时间(毫秒)',
                                   `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_user_id` (`user_id`) USING BTREE,
                                   KEY `idx_created_at` (`created_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话历史记录表';
