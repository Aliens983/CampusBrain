package com.laoliu.cas.appointment.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启预约模块的定时任务（用于咨询/设备预约"到点自动结束"）
 *
 * @author forever-king
 */
@Configuration
@EnableScheduling
public class AppointmentScheduleConfig {
}
