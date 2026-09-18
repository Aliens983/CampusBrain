package com.laoliu.cas.appointment.infrastructure.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/**
 * 预约变更事件发布器，向 RabbitMQ 发布事件，供 KB 服务消费以更新索引/缓存
 * <p>
 * 拓扑由 {@link RabbitMqConfig} 显式声明（DirectExchange + Binding），
 * 不再依赖默认 exchange 的隐式绑定；消息体走 JSON 序列化并声明 content-type。
 * <p>
 * <b>发送时机（12-08）</b>：调用方均在 {@code @Transactional} 业务方法内。
 * 此前在事务提交前同步直发，存在两类问题：
 * <ol>
 *   <li>业务事务最终回滚，但"已预约/已取消"事件已经发出，KB 侧按从未发生的变更淘汰缓存
 *       （且未来若有消费方据此写投影，会产生脏数据）；</li>
 *   <li>Broker 故障（连接拒绝/超时）抛 AmqpException 会直接导致下单/取消事务回滚，
 *       旁路基础设施故障拖垮核心业务。</li>
 * </ol>
 * 现统一为：存在活动事务时注册 {@link TransactionSynchronization#afterCommit()}，
 * 仅在事务成功提交后发送；无事务上下文（如测试/手工调用）时立即发送。
 * 发送异常（含 Broker 不可用）只记日志不外抛——事件旁路不得影响主流程，
 * 缓存淘汰的最终兜底由问答缓存各层 TTL 承担。
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
        final String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("预约变更事件序列化失败，已丢弃: eventType={}, userId={}, serviceId={}",
                    eventType, userId, serviceId, e);
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // 事务提交成功后才发：回滚不发，杜绝"业务没生效、事件已出"
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(payload, eventType, userId, serviceId);
                }
            });
        } else {
            // 无事务上下文（非事务调用/单元测试）：保持原有立即发送语义
            send(payload, eventType, userId, serviceId);
        }
    }

    /**
     * 实际发送。Broker 故障只记日志：afterCommit 阶段事务已无法回滚，
     * 即使在无事务场景下，事件旁路也不应拖垮预约主流程。
     */
    private void send(String payload, String eventType, Long userId, Long serviceId) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, payload, message -> {
                message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                return message;
            });
            log.info("已发布预约变更事件: {}", payload);
        } catch (AmqpException e) {
            // Broker 不可用等发送失败：缓存淘汰缺失由问答缓存 TTL 兜底，不影响预约结果
            log.error("预约变更事件发送失败（依赖缓存 TTL 兜底），已丢弃: eventType={}, userId={}, serviceId={}",
                    eventType, userId, serviceId, e);
        }
    }
}
