package com.laoliu.cas.system.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 图形验证码响应 VO。
 *
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "图形验证码响应")
public class CaptchaRespVO implements Serializable {

    @Schema(description = "验证码唯一标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "abc123-uuid")
    private String uuid;

    @Schema(description = "验证码图片URL", requiredMode = Schema.RequiredMode.REQUIRED, example = "/uploads/captcha/abc123.png")
    private String imageUrl;

}
