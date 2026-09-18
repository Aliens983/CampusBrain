-- 通用/活动预约幂等兜底：
-- INSERT ... SELECT WHERE NOT EXISTS 在 REPEATABLE READ 下是快照读，
-- 双事务并发窗口（同一用户同一服务同时点击/双端并发）可能同时通过 NOT EXISTS 重复落单。
-- 该唯一约束让"待审核/已通过"同档位订单在 DB 层强制唯一；
-- 配合 ItemMapper.insertSingleService 的 INSERT IGNORE，冲突时影响 0 行，
-- 业务层"去重回补库存"（releaseStock）流程保持不变。
-- manage_status 参与唯一键：审核流转（待审→通过/拒绝/取消）后键值变化不冲突，
-- 用户取消（status=3）后可重新预约同一服务。
ALTER TABLE item
    ADD CONSTRAINT uk_user_service_status UNIQUE (user_id, service_id, manage_status);