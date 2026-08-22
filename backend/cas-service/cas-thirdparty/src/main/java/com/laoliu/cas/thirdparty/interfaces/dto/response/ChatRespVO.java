package com.laoliu.cas.thirdparty.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author forever-king
 */
@Schema(description = "聊天响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRespVO {

    @Schema(description = "大模型返回的响应内容")
    private String response;

    @Schema(description = "请求是否成功")
    private boolean success;

    @Schema(description = "错误信息（请求失败时返回）")
    private String errorMessage;

    @Schema(description = "使用的模型名称")
    private String model;

    @Schema(description = "响应时间（毫秒）")
    private Integer responseTimeMs;

    public ChatRespVO(String response, String model, Integer responseTimeMs) {
        this.response = response;
        this.model = model;
        this.responseTimeMs = responseTimeMs;
        this.success = true;
    }

    public ChatRespVO(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }
}
