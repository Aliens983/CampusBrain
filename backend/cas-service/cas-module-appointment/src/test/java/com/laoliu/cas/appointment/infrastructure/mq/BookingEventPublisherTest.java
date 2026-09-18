package com.laoliu.cas.appointment.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link BookingEventPublisher} 事务提交后发送语义测试（12-08）。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("预约事件事务提交后发送测试")
class BookingEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private BookingEventPublisher publisher;

    @BeforeEach
    void setUp() {
        // 与生产 Spring 容器中的 ObjectMapper 对齐：事件体含 LocalDateTime，
        // 不注册 JavaTimeModule 会抛 InvalidDefinitionException（被测代码将其吞掉并零交互）
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        publisher = new BookingEventPublisher(rabbitTemplate, objectMapper);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    @DisplayName("活动事务内：提交前不发送，afterCommit 触发后才发送一次")
    void sendsOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        publisher.publishChanged(1001L, 2002L, "BOOKED");

        verify(rabbitTemplate, never()).convertAndSend(
                anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));

        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        assertThat(syncs).hasSize(1);
        syncs.get(0).afterCommit();

        verify(rabbitTemplate).convertAndSend(
                eq(BookingEventPublisher.EXCHANGE),
                eq(BookingEventPublisher.ROUTING_KEY),
                any(Object.class), any(MessagePostProcessor.class));
    }

    @Test
    @DisplayName("活动事务内：仅注册不触发 afterCommit（模拟回滚）时不发送")
    void doesNotSendWhenTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();

        publisher.publishChanged(1001L, 2002L, "CANCELLED");

        // 回滚场景：afterCommit 不会被回调，只清理同步器
        TransactionSynchronizationManager.clear();

        verify(rabbitTemplate, never()).convertAndSend(
                anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));
    }

    @Test
    @DisplayName("无事务上下文：保持立即发送语义")
    void sendsImmediatelyWithoutTransaction() {
        publisher.publishChanged(1001L, 2002L, "BOOKED");

        verify(rabbitTemplate).convertAndSend(
                eq(BookingEventPublisher.EXCHANGE),
                eq(BookingEventPublisher.ROUTING_KEY),
                any(Object.class), any(MessagePostProcessor.class));
    }

    @Test
    @DisplayName("Broker 故障：发送异常被吞掉，不外抛影响主流程")
    void brokerFailureIsSwallowed() {
        doThrow(new AmqpException("connection refused"))
                .when(rabbitTemplate).convertAndSend(
                        anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));

        assertThatCode(() -> publisher.publishChanged(1L, 2L, "BOOKED"))
                .doesNotThrowAnyException();
    }
}
