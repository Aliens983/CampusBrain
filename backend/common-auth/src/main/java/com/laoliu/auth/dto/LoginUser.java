package com.laoliu.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 认证后的登录用户上下文（common-auth 自包含，供网关解析 JWT 时使用）。
 *
 * @author forever-king
 */
@Data
public class LoginUser implements Serializable {

    private Long id;

    private String name;

    private Integer role;

    private String email;
}
