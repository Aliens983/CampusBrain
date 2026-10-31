package com.laoliu.cas.common.time;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 业务时区唯一入口.
 * <p>
 * 容器默认时区可能是 UTC，而 MySQL 使用东八区；直接调用
 * {@code LocalDate.now()} 会在北京时间 00:00–08:00 得到前一天，
 * 导致预约窗口、排班生成、助手日期解析整体偏移 8 小时.
 * 所有业务“今天/现在”判断统一走本类，避免依赖宿主机/容器时区.
 * </p>
 *
 * @author forever-king
 */
public final class BusinessTime {

    /** 业务时区：杭州师范大学所在东八区 */
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private BusinessTime() {
    }

    /** 业务今天 */
    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    /** 业务今天（可注入时钟，便于测试跨时区边界） */
    public static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(ZONE));
    }

    /** 业务当前时刻（含日期时间） */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    /** 业务当前时刻（可注入时钟） */
    public static LocalDateTime now(Clock clock) {
        return LocalDateTime.now(clock.withZone(ZONE));
    }

    /** 业务当前时间（时分秒） */
    public static LocalTime nowTime() {
        return LocalTime.now(ZONE);
    }

    /**
     * 业务时区时钟；需要注入 Clock 的组件/测试使用，避免硬编码.
     */
    public static Clock clock() {
        return Clock.system(ZONE);
    }
}
