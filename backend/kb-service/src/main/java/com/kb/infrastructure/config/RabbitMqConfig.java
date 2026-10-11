package com.kb.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * RabbitMQ 消息队列配置
 * <p>
 * 定义文档异步处理的消息队列：上传 → 发消息 → 消费者异步执行解析/分块/Embedding/入库
 * </p>
 *
 * @author forever-king
 */
@Configuration
public class RabbitMqConfig {

    /** 交换机名称 */
    public static final String EXCHANGE_DOCUMENT = "kb.document.exchange";

    /** 文档处理队列名称 */
    public static final String QUEUE_DOCUMENT_PROCESSING = "kb.document.processing.queue";

    /** 路由键 */
    public static final String ROUTING_KEY_DOCUMENT_PROCESSING = "kb.document.processing";

    /** 死信交换机 / 死信队列 / 死信路由键（12-03）：
     * 消费重试耗尽（且不 requeue）的消息进入 DLQ 留存待人工/补偿处理，而不是直接丢弃 */
    public static final String DLX_DOCUMENT = "kb.document.exchange.dlx";
    public static final String QUEUE_DOCUMENT_PROCESSING_DLQ = "kb.document.processing.queue.dlq";
    public static final String ROUTING_KEY_DOCUMENT_DLQ = "kb.document.processing.dlq";

    @Bean
    public DirectExchange documentExchange() {
        return new DirectExchange(EXCHANGE_DOCUMENT, true, false);
    }

    /** 死信交换机（持久化） */
    @Bean
    public DirectExchange documentDlx() {
        return new DirectExchange(DLX_DOCUMENT, true, false);
    }

    /**
     * 文档处理主队列：绑定死信交换机。
     * <p>
     * 配合 listener retry（3 次指数退避）+ default-requeue-rejected=false：
     * 可重试异常在本实例重试 3 次仍失败后，消息被拒绝并路由到 DLQ，不再静默丢弃。
     */
    @Bean
    public Queue documentProcessingQueue() {
        return QueueBuilder.durable(QUEUE_DOCUMENT_PROCESSING)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", DLX_DOCUMENT,
                        "x-dead-letter-routing-key", ROUTING_KEY_DOCUMENT_DLQ))
                .build();
    }

    /** 死信队列：留存处理失败的文档消息，供监控与人工重放 */
    @Bean
    public Queue documentProcessingDlq() {
        return QueueBuilder.durable(QUEUE_DOCUMENT_PROCESSING_DLQ).build();
    }

    @Bean
    public Binding documentProcessingBinding() {
        return BindingBuilder
                .bind(documentProcessingQueue())
                .to(documentExchange())
                .with(ROUTING_KEY_DOCUMENT_PROCESSING);
    }

    @Bean
    public Binding documentDlqBinding() {
        return BindingBuilder
                .bind(documentProcessingDlq())
                .to(documentDlx())
                .with(ROUTING_KEY_DOCUMENT_DLQ);
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
     * 监听容器工厂 — 让 @RabbitListener 也能用 JSON 转换器。
     * <p>
     * 必须经 {@link SimpleRabbitListenerContainerFactoryConfigurer} 应用
     * {@code spring.rabbitmq.listener.simple.*} 配置：此前手工 new 工厂并顶掉了
     * Boot 自动配置的同名 bean，导致 yml 中的 concurrency/max-concurrency/prefetch/
     * retry 退避全部静默失效（实际单消费者、首次失败直接进 DLQ）。
     * 自定义项（JSON 转换器、requeue 策略）在 configure 之后设置，保证不被覆盖。
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        // 应用 yml：并发 3~5、prefetch 2、retry 3 次指数退避、测试环境 auto-startup=false 等
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);
        // 本地重试耗尽后不 requeue（毒消息不再无限循环），拒绝后由队列 DLX 路由进 DLQ
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
