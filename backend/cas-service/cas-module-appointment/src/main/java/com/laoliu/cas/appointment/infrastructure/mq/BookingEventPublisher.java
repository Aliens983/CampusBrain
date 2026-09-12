package com.laoliu.cas.appointment.infrastructure.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 预约变更事件发布器，向 RabbitMQ 发布事件，供 KB 服务消费以更新索引/缓存
 * <p>
 * 拓扑由 {@link RabbitMqConfig} 显式声明（DirectExchange + Binding），
 * 不再依赖默认 exchange 的隐式绑定；消息体走 JSON 序列化并声明 content-type。
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventPublisher {

    /** 交换机名（显式声明，取代此前的默认 exchange 直投） */
    public static final String EXCHANGE = "cas.appointment.exchange";

    /** 队列名（沿用历史名称，避免存量消息丢失） */
    public static final String QUEUE = "appointment.changed";

    /** 路由键 */
    public static final String ROUTING_KEY = "appointment.changed";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 发布预约变更事件。
     * <p>
     * 序列化失败只记日志、不抛异常：事件发布属于旁路逻辑，
     * 不应因为序列化问题让预约主流程失败。
     */
    public void publishChanged(Long userId, Long serviceId, String eventType) {
        AppointmentChangedEvent event = AppointmentChangedEvent.builder()
                .eventType(eventType)
                .userId(userId)
                .serviceId(serviceId)
                .occurredAt(LocalDateTime.now())
                .build();
        try {
            String payload = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, payload, message -> {
                message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                return message;
            });
            log.info("已发布预约变更事件: {}", payload);
        } catch (JsonProcessingException e) {
            log.error("预约变更事件序列化失败，已丢弃: eventType={}, userId={}, serviceId={}",
                    eventType, userId, serviceId, e);
        }
    }
}
