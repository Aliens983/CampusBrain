package com.laoliu.cas.appointment.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 预约模块可配置参数。
 *
 * @author forever-king
 */
@Data
@Component
@ConfigurationProperties(prefix = "booking")
public class BookingProperties {

    /**
     * 幂等去重时间窗口（秒）。
     * <p>
     * 同一用户对同一资源（咨询时段/设备窗口/教室窗口，以及活动免审直通）在该窗口内的
     * 待审核重复提交会被 SQL 去重。此前 60 秒魔法数字散落在 4~5 处 SQL 中，无法按环境调整，
     * 现统一收敛到此配置，默认仍为 60 秒以保持原行为。
     */
    private int dedupeSeconds = 60;
}
