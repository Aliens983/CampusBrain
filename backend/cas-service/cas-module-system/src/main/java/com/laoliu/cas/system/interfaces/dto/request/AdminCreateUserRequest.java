package com.laoliu.cas.system.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员创建用户请求
 *
 * @author forever-king
 */
@Data
@Schema(description = "管理员创建用户请求")
public class AdminCreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度必须在2-50之间")
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String name;

    @Schema(description = "年级", example = "大一")
    private String grade;

    @Schema(description = "性别", example = "男")
    private String sex;

    @Schema(description = "年龄", example = "18")
    private Integer age;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "user@example.com")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100之间")
    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "password123")
    private String password;

    @Min(value = 0, message = "角色值必须为0/1/2")
    @Max(value = 2, message = "角色值必须为0/1/2")
    @Schema(description = "角色（0=普通用户, 1=管理员, 2=超级管理员）", example = "1")
    private Integer role;
}
