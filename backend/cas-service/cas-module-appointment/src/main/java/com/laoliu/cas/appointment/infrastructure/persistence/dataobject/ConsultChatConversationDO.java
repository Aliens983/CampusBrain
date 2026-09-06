package com.laoliu.cas.appointment.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laoliu.cas.appointment.domain.entity.ConsultChatConversation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 咨询沟通会话数据对象 - MyBatis-Plus ORM（consult_chat_conversation）
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("consult_chat_conversation")
public class ConsultChatConversationDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long teacherId;

    private LocalDateTime createdAt;

    public ConsultChatConversation toEntity() {
        return ConsultChatConversation.builder()
                .id(id).studentId(studentId).teacherId(teacherId).createdAt(createdAt)
                .build();
    }

    public static ConsultChatConversationDO from(ConsultChatConversation conversation) {
        return ConsultChatConversationDO.builder()
                .id(conversation.getId())
                .studentId(conversation.getStudentId())
                .teacherId(conversation.getTeacherId())
                .createdAt(conversation.getCreatedAt())
                .build();
    }
}
