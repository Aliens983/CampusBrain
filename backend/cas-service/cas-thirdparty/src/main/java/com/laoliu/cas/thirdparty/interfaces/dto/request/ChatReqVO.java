package com.laoliu.cas.thirdparty.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天请求
 *
 * @author forever-king
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "聊天请求")
public class ChatReqVO {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 4000, message = "消息内容不能超过4000字符")
    @Schema(description = "发送给大模型的消息", requiredMode = Schema.RequiredMode.REQUIRED, example = "你好，请介绍一下你自己")
    private String message;

    @Schema(description = "模型名称（可选，默认qwen-plus）", example = "qwen-plus")
    private String model;

    public ChatReqVO(String message) {
        this.message = message;
        this.model = "qwen-plus";
    }

}
