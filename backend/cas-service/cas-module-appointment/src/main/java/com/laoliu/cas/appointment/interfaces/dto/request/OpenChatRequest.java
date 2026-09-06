package com.laoliu.cas.appointment.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 打开咨询会话请求（凭咨询师卡片，学生侧使用）
 * <p>
 * 学生从「选咨询师」卡片发起时传咨询师ID，后端解析该咨询师绑定的教师账号并建会话。
 *
 * @author forever-king
 */
@Data
@Schema(description = "按咨询师打开咨询会话请求")
public class OpenChatRequest {

    @NotNull(message = "请选择咨询师")
    @Schema(description = "咨询师ID（consultant.id）")
    private Long consultantId;
}
