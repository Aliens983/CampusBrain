package com.laoliu.cas.appointment.infrastructure.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 预约变更事件发布器，向 RabbitMQ 发布事件，供 KB 服务消费以更新索引/缓存
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventPublisher {

    public static final String QUEUE = "appointment.changed";

    private final RabbitTemplate rabbitTemplate;

    public void publishChanged(Long userId, Long serviceId, String eventType) {
        String payload = String.format(
                "{\"eventType\":\"%s\",\"userId\":%d,\"serviceId\":%d}",
                eventType, userId, serviceId);
        rabbitTemplate.convertAndSend(QUEUE, payload);
        log.info("已发布预约变更事件: {}", payload);
    }
}
