package com.kb.interfaces.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录响应 DTO（含双 Token）
 *
 * @author forever-king
 */
@Data
@Builder
public class LoginResponse {

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 用户角色 */
    private String role;

    /** 用户昵称 */
    private String nickname;

    /** 访问令牌（短有效期） */
    private String accessToken;

    /** 刷新令牌（长有效期） */
    private String refreshToken;

    /** 访问令牌过期时间 */
    private LocalDateTime expiresTime;
}
