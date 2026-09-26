package com.kb.infrastructure.persistence.mysql.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话摘要 DO：conversation 表按 session_id 聚合的结果行（非表映射）。
 *
 * @author forever-king
 */
@Data
public class ConversationSummaryDO {

    /** 会话 ID */
    private String sessionId;

    /** 会话标题：该会话第一条用户消息原文 */
    private String title;

    /** 最后一条消息时间 */
    private LocalDateTime updatedAt;
}
