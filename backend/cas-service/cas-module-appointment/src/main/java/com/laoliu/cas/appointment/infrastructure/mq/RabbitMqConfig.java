package com.laoliu.cas.appointment.infrastructure.mq;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 队列声明
 *
 * @author forever-king
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public Queue appointmentChangedQueue() {
        return new Queue(BookingEventPublisher.QUEUE, true);
    }
}
