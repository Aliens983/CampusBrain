package com.kb.infrastructure.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Message payload for asynchronous document processing.
 *
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProcessingMessage implements Serializable {

    /** 序列化版本UID */
    private static final long serialVersionUID = 1L;

    /** 待处理的文档ID */
    private Long documentId;

    /** 是否强制重新处理（即使已处于READY状态） */
    private boolean forceReprocess;

    /** 租户 ID（跨线程传递，消费者线程据此恢复 TenantContext） */
    private Long tenantId;
}
