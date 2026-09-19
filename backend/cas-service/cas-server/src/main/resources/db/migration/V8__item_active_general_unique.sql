-- ============================================================================
-- V8：修正 V7 唯一约束 uk_user_service_status 的三重故障（深度审查报告 3.1，D-A1）
--
-- V7 旧约束 UNIQUE (user_id, service_id, manage_status) 的问题：
--   故障 A：同一用户对同一服务第二次取消/完结/拒绝时，终态行 (u,s,3)/(u,s,4)
--           已存在 → 裸 UPDATE 撞 1062，取消失败、库存回滚；
--   故障 B：资源类预约（咨询/教室/设备）同一服务的不同资源（如同服务的两间
--           不同教室）第二笔待审单直接 DuplicateKeyException → 500；
--   故障 C：存量库只要有任一重复组合，V7 的 ALTER 就失败 → Flyway 卡死、起不来。
--
-- 修正：生成列 active_dedup 只标识"通用/活动类的有效态单"：
--   * manage_status IN (0,1)（待审核/已通过）且四个资源外键全为空 → 0；
--   * 终态单（2/3/4）与资源类单（任一资源外键非空）→ NULL。
-- 唯一索引中 NULL 互不相等，因此：
--   1. 终态行可无限共存 → 故障 A 消失；
--   2. 资源类单不参与唯一约束 → 故障 B 消失；
--   3. 存量脏数据全部映射 NULL，加列加索引不再被重复数据阻塞 → 故障 C 消失。
-- 0/1 统一映射为 0，与 insertSingleService 的 NOT EXISTS(status IN (0,1)) 严格对齐。
--
-- Flyway 迁移链保证 V7 已先执行，旧索引必然存在，直接 DROP。
-- ============================================================================

ALTER TABLE item
    DROP INDEX uk_user_service_status;

ALTER TABLE item
    ADD COLUMN active_dedup TINYINT AS (
        CASE
            WHEN manage_status IN (0, 1)
                 AND consultant_id IS NULL
                 AND slot_id IS NULL
                 AND room_id IS NULL
                 AND equipment_id IS NULL
                THEN 0
            ELSE NULL
            END
        ) VIRTUAL COMMENT '通用/活动类有效态去重标记：0=参与唯一约束，NULL=终态或资源类不约束',
    ADD CONSTRAINT uk_item_active_general UNIQUE (user_id, service_id, active_dedup);
