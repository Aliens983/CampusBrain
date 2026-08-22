package com.laoliu.cas.system.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "邮件响应")
public class EmailResponse {

    @Schema(description = "响应消息", example = "邮件发送成功")
    private String message;
}
