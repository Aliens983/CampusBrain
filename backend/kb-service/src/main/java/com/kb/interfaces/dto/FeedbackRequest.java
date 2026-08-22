package com.kb.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * User feedback request DTO.
 * @author forever-king
 */
@Data
public class FeedbackRequest {

    /** 会话ID */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /** 消息ID */
    @NotNull(message = "消息ID不能为空")
    private Long messageId;

    /** 反馈内容，取值为 "like"（点赞）或 "dislike"（踩） */
    @NotBlank(message = "反馈内容不能为空")
    private String feedback;
}
