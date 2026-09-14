package com.kb.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.infrastructure.cache.QaCacheService;
import com.kb.infrastructure.cache.SemanticCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * 预约变更事件消费端（来自 CAS）
 * <p>
 * CAS 在预约创建/取消时发布事件，KB 消费后可用于更新知识索引或缓存。
 * <p>
 * 拓扑与 CAS 侧显式对齐（同名 DirectExchange + routingKey），
 * 不再依赖默认 exchange 的隐式绑定。
 *
 * @author forever-king
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AppointmentEventConfig {

    /** 与 CAS 侧 BookingEventPublisher 的常量保持一致，改名需两边同步 */
    public static final String EXCHANGE = "cas.appointment.exchange";
    public static final String QUEUE = "appointment.changed";
    public static final String ROUTING_KEY = "appointment.changed";

    private final ObjectMapper objectMapper;
    private final QaCacheService qaCacheService;
    private final SemanticCacheService semanticCacheService;

    @Bean
    public DirectExchange appointmentExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue appointmentChangedQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding appointmentChangedBinding() {
        return BindingBuilder
                .bind(appointmentChangedQueue())
                .to(appointmentExchange())
                .with(ROUTING_KEY);
    }

    /**
     * 消费预约变更事件。
     * <p>
     * 接收原始 {@link Message} 再自行解析，而不是直接声明 DTO 入参：
     * 既能兼容历史遗留的 text/plain 消息，也能保证单条脏数据只记日志丢弃，
     * 不会因反序列化异常导致消息反复重投、卡住队列。
     */
    @RabbitListener(queues = QUEUE)
    public void onAppointmentChanged(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            AppointmentChangedEvent event = objectMapper.readValue(body, AppointmentChangedEvent.class);
            log.info("KB 收到预约变更事件: eventType={}, userId={}, serviceId={}, occurredAt={}",
                    event.getEventType(), event.getUserId(), event.getServiceId(), event.getOccurredAt());
            // 3.1.1：预约创建/取消会改变实时余量、可约状态与"我的预约"，
            // 失效精确 + 语义两层问答缓存，避免把过期答案继续返回给用户。
            // 失效失败只记日志：缓存是可重建的派生数据，不应让消费异常反复重投卡住队列。
            try {
                qaCacheService.evictAll();
                semanticCacheService.evictAll();
            } catch (Exception cacheEx) {
                log.warn("预约变更联动失效问答缓存失败，将依赖缓存 TTL 自然过期", cacheEx);
            }
        } catch (Exception e) {
            log.error("解析预约变更事件失败，已丢弃该消息: {}", body, e);
        }
    }
}
