package com.kb.infrastructure.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 预约变更事件消费端（来自 CAS）。
 * <p>
 * CAS 在预约创建/取消时发布事件，KB 消费后可用于更新知识索引或缓存。
 *
 * @author forever-king
 */
@Slf4j
@Configuration
public class AppointmentEventConfig {

    public static final String QUEUE = "appointment.changed";

    @Bean
    public Queue appointmentChangedQueue() {
        return new Queue(QUEUE, true);
    }

    @RabbitListener(queues = QUEUE)
    public void onAppointmentChanged(String message) {
        log.info("KB 收到预约变更事件: {}", message);
        // TODO: 后续可据此更新知识索引 / 缓存
    }
}
