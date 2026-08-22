package com.kb.interfaces.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 邮箱验证码注册请求
 *
 * @author forever-king
 */
@Data
public class RegisterRequest {

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度为2-50个字符")
    private String username;

    /** 邮箱 */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 6 位邮箱验证码 */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码为6位数字")
    private String code;

    /** 密码（必须包含大小写字母和数字） */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 100, message = "密码长度为8-100个字符")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "密码必须包含大写字母、小写字母和数字")
    private String password;
}
