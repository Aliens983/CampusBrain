package com.laoliu.cas.thirdparty.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laoliu.cas.thirdparty.domain.entity.AiChatHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 对话历史数据对象 - 用于 MyBatis-Plus ORM
 *
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("ai_chat_history")
public class AiChatHistoryDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 模型名称（如 qwen-plus） */
    private String model;

    /** 用户发送的消息内容 */
    private String userMessage;

    /** AI 回复的内容 */
    private String aiResponse;

    /** 响应耗时（毫秒） */
    private Integer responseTimeMs;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    public AiChatHistory toEntity() {
        return AiChatHistory.builder()
                .id(id).userId(userId).model(model).userMessage(userMessage)
                .aiResponse(aiResponse).responseTimeMs(responseTimeMs)
                .createdAt(createdAt).updatedAt(updatedAt)
                .build();
    }

    public static AiChatHistoryDO fromEntity(AiChatHistory entity) {
        if (entity == null) return null;
        return AiChatHistoryDO.builder()
                .id(entity.getId()).userId(entity.getUserId()).model(entity.getModel())
                .userMessage(entity.getUserMessage()).aiResponse(entity.getAiResponse())
                .responseTimeMs(entity.getResponseTimeMs()).createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
