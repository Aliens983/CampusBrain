package com.kb.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消息队列配置
 * <p>
 * 定义文档异步处理的消息队列：上传 → 发消息 → 消费者异步执行解析/分块/Embedding/入库。
 * </p>
 *
 * @author forever-king
 */
@Configuration
public class RabbitMqConfig {

    /** 监听容器是否自动启动（测试环境可关闭，避免连接真实 RabbitMQ） */
    @Value("${spring.rabbitmq.listener.simple.auto-startup:true}")
    private boolean listenerAutoStartup;

    /** 交换机名称 */
    public static final String EXCHANGE_DOCUMENT = "kb.document.exchange";

    /** 文档处理队列名称 */
    public static final String QUEUE_DOCUMENT_PROCESSING = "kb.document.processing.queue";

    /** 路由键 */
    public static final String ROUTING_KEY_DOCUMENT_PROCESSING = "kb.document.processing";

    @Bean
    public DirectExchange documentExchange() {
        return new DirectExchange(EXCHANGE_DOCUMENT, true, false);
    }

    @Bean
    public Queue documentProcessingQueue() {
        return new Queue(QUEUE_DOCUMENT_PROCESSING, true, false, false);
    }

    @Bean
    public Binding documentProcessingBinding() {
        return BindingBuilder
                .bind(documentProcessingQueue())
                .to(documentExchange())
                .with(ROUTING_KEY_DOCUMENT_PROCESSING);
    }

    /**
     * JSON 消息转换器 — 生产者和消费者共用
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setCreateMessageIds(true);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    /**
     * 监听容器工厂 — 让 @RabbitListener 也能用 JSON 转换器
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setAutoStartup(listenerAutoStartup);
        return factory;
    }
}
