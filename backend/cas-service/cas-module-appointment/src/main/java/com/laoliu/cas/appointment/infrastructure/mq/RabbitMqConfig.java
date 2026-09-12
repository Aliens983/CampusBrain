package com.laoliu.cas.appointment.infrastructure.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑声明
 * <p>
 * 显式声明 DirectExchange + Queue + Binding，取代此前"只声明队列、
 * 靠默认 exchange 隐式绑定"的方式——隐式绑定要求 routingKey 恰好等于队列名，
 * 一旦任一侧改名就会静默失效。
 *
 * @author forever-king
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange appointmentExchange() {
        return new DirectExchange(BookingEventPublisher.EXCHANGE, true, false);
    }

    @Bean
    public Queue appointmentChangedQueue() {
        return new Queue(BookingEventPublisher.QUEUE, true);
    }

    @Bean
    public Binding appointmentChangedBinding() {
        return BindingBuilder
                .bind(appointmentChangedQueue())
                .to(appointmentExchange())
                .with(BookingEventPublisher.ROUTING_KEY);
    }
}
