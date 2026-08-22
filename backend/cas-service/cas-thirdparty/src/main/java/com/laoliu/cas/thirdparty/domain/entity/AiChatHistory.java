package com.laoliu.cas.thirdparty.domain.entity;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 对话历史领域实体
 * 纯净，不依赖任何框架注解
 *
 * @author forever-king
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode()
@ToString
@Builder
public class AiChatHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 对话ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 模型名称 */
    private String model;

    /** 用户消息 */
    private String userMessage;

    /** AI回复 */
    private String aiResponse;

    /** 响应时间(毫秒) */
    private Integer responseTimeMs;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
