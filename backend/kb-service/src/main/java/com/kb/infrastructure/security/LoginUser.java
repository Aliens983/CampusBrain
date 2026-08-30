package com.kb.infrastructure.security;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * 内存中的用户模型
 * <p>
 * 每次请求校验 Access Token 后构建，存入 Spring Security 上下文
 * 后续业务代码通过 SecurityFrameworkUtils.getLoginUser() 获取
 * </p>
 *
 * @author forever-king
 */
@Getter
@Builder
public class LoginUser {

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 用户角色（ADMIN / USER） */
    private String role;

    /** 昵称 */
    private String nickname;

    /**
     * 转换为 Spring Security 权限列表
     */
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
