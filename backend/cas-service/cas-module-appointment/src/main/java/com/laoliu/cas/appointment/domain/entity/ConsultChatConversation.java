package com.laoliu.cas.appointment.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 咨询沟通会话（一个学生 ⇄ 一位咨询教师 的唯一一条持续会话）
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultChatConversation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话ID */
    private Long id;

    /** 学生用户ID（user.id，代码级外键） */
    private Long studentId;

    /** 咨询教师用户ID（user.id，须为某「教师咨询」分类咨询师绑定账号） */
    private Long teacherId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
