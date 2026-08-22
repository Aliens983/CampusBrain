package com.kb.infrastructure.mq;

import com.kb.infrastructure.config.RabbitMqConfig;
import com.kb.infrastructure.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Producer: sends document processing messages to RabbitMQ.
 * <p>
 * After a document is uploaded to MinIO, a message is sent to trigger
 * the async processing pipeline: Parse → Chunk → Embed → Store.
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingProducer {

    /** RabbitMQ消息发送模板 */
    private final RabbitTemplate rabbitTemplate;

    /**
     * Enqueue a document for async processing.
     */
    public void send(Long documentId) {
        send(documentId, false);
    }

    /**
     * Enqueue a document for async processing, optionally forcing re-processing.
     */
    public void send(Long documentId, boolean forceReprocess) {
        DocumentProcessingMessage message = new DocumentProcessingMessage(
                documentId, forceReprocess, TenantContext.getTenantId());
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE_DOCUMENT,
                RabbitMqConfig.ROUTING_KEY_DOCUMENT_PROCESSING,
                message
        );
        log.info("Sent document processing message: documentId={}", documentId);
    }
}
