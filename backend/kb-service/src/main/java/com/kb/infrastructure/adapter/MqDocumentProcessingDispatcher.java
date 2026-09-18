package com.kb.infrastructure.adapter;

import com.kb.domain.document.DocumentProcessingDispatcher;
import com.kb.infrastructure.mq.DocumentProcessingProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 文档处理投递端口的 MQ 实现适配器。
 * <p>
 * {@link DocumentProcessingProducer} 位于基础设施 mq 包（跨 AI 协作中归属对方），
 * 本适配器仅做委托转发，不改动其源码，让应用层经 {@link DocumentProcessingDispatcher}
 * 使用它。行为与原调用完全一致（含投递失败由调用方重试的约定）。
 * </p>
 *
 * @author forever-king
 */
@Component
@RequiredArgsConstructor
public class MqDocumentProcessingDispatcher implements DocumentProcessingDispatcher {

    private final DocumentProcessingProducer delegate;

    @Override
    public void send(Long documentId) {
        delegate.send(documentId);
    }
}