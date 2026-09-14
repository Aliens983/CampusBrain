-- 3.1.2 通用/活动预约自动完结闭环：
-- 给 services 增加 end_date（活动/通用服务完结日期）。
-- 无时段（item.start_time/end_time 为空）的已通过预约单由定时任务据此自动完结：
-- end_date 早于当天即完成；NULL 表示长期有效服务，不自动完结。
ALTER TABLE services
    ADD COLUMN end_date DATE NULL COMMENT '活动/通用服务完结日期（次日零点后，无时段的已通过预约自动完成；NULL=长期有效，不自动完结）'
    AFTER capacity;

-- 3.1.7 库存口径语义澄清：
-- booked_count 仅对走 bookService 下单链路的容量型服务（活动报名/通用服务）做乐观锁扣减；
-- 咨询/教室/设备为窗口型资源，可约量按时间窗口动态计算（time_slot.available / overlap 统计），此列对它们恒为 0。
ALTER TABLE services
    MODIFY booked_count INT NOT NULL DEFAULT 0
    COMMENT '容量型服务(活动/通用)有效预约数，乐观锁扣减；窗口型资源(咨询/教室/设备)按时段动态计算，此列恒0';
