package com.laoliu.cas.system.application.service.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author forever-king
 */
@Data
@Schema(description = "用户注册VO")
public class UserRegisterVO {

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String name;

    @Schema(description = "年级", requiredMode = Schema.RequiredMode.REQUIRED, example = "大一")
    private String grade;

    @Schema(description = "性别", requiredMode = Schema.RequiredMode.REQUIRED, example = "男")
    private String sex;

    @Schema(description = "年龄", requiredMode = Schema.RequiredMode.REQUIRED, example = "18")
    private Integer age;

    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "user@example.com")
    private String email;

    @Schema(description = "邮箱验证码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    private String code;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "password123")
    private String password;
}
