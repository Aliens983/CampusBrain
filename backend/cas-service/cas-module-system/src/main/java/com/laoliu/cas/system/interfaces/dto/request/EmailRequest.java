package com.laoliu.cas.system.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 邮件发送请求
 *
 * @author forever-king
 */
@Data
@Schema(description = "邮件发送请求")
public class EmailRequest {

    @NotBlank(message = "收件人邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "收件人邮箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "user@example.com")
    private String to;
}
