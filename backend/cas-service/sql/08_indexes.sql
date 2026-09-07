-- ============================================================
-- 数据库索引添加脚本
-- 为常用查询字段添加索引以提高查询性能
-- ============================================================

-- user 表：邮箱常用于登录/注册查询
CREATE INDEX IF NOT EXISTS idx_user_email ON `user`(`email`);

-- services 表：服务状态常用于筛选可用服务
CREATE INDEX IF NOT EXISTS idx_services_state ON `services`(`service_state`);

-- item 表：用户ID和订单状态常用于查询
CREATE INDEX IF NOT EXISTS idx_item_user_id ON `item`(`user_id`);
CREATE INDEX IF NOT EXISTS idx_item_service_id ON `item`(`service_id`);
CREATE INDEX IF NOT EXISTS idx_item_manage_status ON `item`(`manage_status`);
CREATE INDEX IF NOT EXISTS idx_item_user_status ON `item`(`user_id`, `manage_status`);

