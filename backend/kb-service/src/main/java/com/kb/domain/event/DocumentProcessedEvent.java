package com.kb.domain.event;

import com.kb.domain.document.DocumentStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 文档处理完成事件
 * <p>
 * 在文档处理流水线完成（READY 或 FAILED）时发布，
 * 消费者可据此发送通知、更新缓存、写审计日志
 * </p>
 * @author forever-king
 */
@Getter
public class DocumentProcessedEvent extends ApplicationEvent {

    private final Long documentId;
    private final String documentTitle;
    private final Long ownerId;
    private final DocumentStatus status;
    private final String errorMsg;

    public DocumentProcessedEvent(Object source, Long documentId, String documentTitle,
                                  Long ownerId, DocumentStatus status, String errorMsg) {
        super(source);
        this.documentId = documentId;
        this.documentTitle = documentTitle;
        this.ownerId = ownerId;
        this.status = status;
        this.errorMsg = errorMsg;
    }

    public boolean isReady() { return status == DocumentStatus.READY; }
    public boolean isFailed() { return status == DocumentStatus.FAILED; }
}
