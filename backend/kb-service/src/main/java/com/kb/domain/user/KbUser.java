package com.kb.domain.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户领域实体
 *
 * @author forever-king
 */
@Data
@Builder
public class KbUser {

    /** 用户唯一标识 */
    private Long id;

    /** 用户名 */
    private String username;

    /** 邮箱 */
    private String email;

    /** BCrypt 加密密码 */
    private String passwordHash;

    /** 昵称 */
    private String nickname;

    /** 角色：ADMIN / USER */
    private String role;

    /** 是否启用 */
    private Boolean enabled;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
