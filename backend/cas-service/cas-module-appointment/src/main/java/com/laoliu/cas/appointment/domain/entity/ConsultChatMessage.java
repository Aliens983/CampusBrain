package com.laoliu.cas.appointment.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 咨询沟通消息
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultChatMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 消息ID */
    private Long id;

    /** 会话ID */
    private Long conversationId;

    /** 发送者用户ID（学生或教师） */
    private Long senderId;

    /** 消息内容 */
    private String content;

    /** 接收方是否已读（1已读 0未读） */
    private Boolean readFlag;

    /** 发送时间 */
    private LocalDateTime createdAt;
}
