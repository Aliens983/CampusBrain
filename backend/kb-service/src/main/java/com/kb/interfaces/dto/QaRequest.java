package com.kb.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Q&A request DTO.
 * @author forever-king
 */
@Data
@Schema(description = "问答请求")
public class QaRequest {

    /** 用户提出的问题 */
    @NotBlank(message = "问题不能为空")
    @Size(max = 2000, message = "问题长度不能超过2000字符")
    @Schema(description = "用户问题", example = "年假有多少天？", maxLength = 2000)
    private String query;

    /** 已有的会话ID，用于多轮对话上下文关联 */
    @Schema(description = "会话 ID（首次提问不传，后续传入返回的 sessionId）", example = "a1b2c3d4")
    private String sessionId;
}
