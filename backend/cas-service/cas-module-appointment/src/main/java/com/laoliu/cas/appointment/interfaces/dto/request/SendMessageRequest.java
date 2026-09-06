package com.laoliu.cas.appointment.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送咨询消息请求
 *
 * @author forever-king
 */
@Data
@Schema(description = "发送咨询消息请求")
public class SendMessageRequest {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容过长")
    @Schema(description = "消息内容")
    private String content;
}
