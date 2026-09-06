package com.laoliu.cas.appointment.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laoliu.cas.appointment.domain.entity.ConsultChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 咨询沟通消息数据对象 - MyBatis-Plus ORM（consult_chat_message）
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("consult_chat_message")
public class ConsultChatMessageDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private Long senderId;

    private String content;

    private Boolean readFlag;

    private LocalDateTime createdAt;

    public ConsultChatMessage toEntity() {
        return ConsultChatMessage.builder()
                .id(id).conversationId(conversationId).senderId(senderId)
                .content(content).readFlag(readFlag).createdAt(createdAt)
                .build();
    }

    public static ConsultChatMessageDO from(ConsultChatMessage message) {
        return ConsultChatMessageDO.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .readFlag(message.getReadFlag() != null && message.getReadFlag())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
