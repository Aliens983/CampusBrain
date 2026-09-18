package com.kb.infrastructure.schedule;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启 Spring 定时任务（KB 服务）。
 * <p>
 * 当前承载：卡死文档超时回收任务（{@link StuckDocumentReclaimer}，12-03）。
 *
 * @author forever-king
 */
@Configuration
@EnableScheduling
public class KbSchedulingConfig {
}
