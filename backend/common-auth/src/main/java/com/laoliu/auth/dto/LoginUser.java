package com.laoliu.auth.dto;

import com.laoliu.auth.policy.RolePolicy;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 认证后的登录用户上下文（全服务唯一模型，Q-02 收敛 common-auth / cas / kb 三份副本）。
 * <p>
 * 线上契约（JWT claim、网关内网头）固定为字段名 {@code id/name/role/email}，
 * 其中 {@link #role} 为数字角色 code（见 {@link RolePolicy}）；
 * 不再各服务自定义字段名（userId/username）或把角色在字符串/数字间反复转换。
 *
 * @author forever-king
 */
@Data
public class LoginUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    /** 数字角色 code，映射唯一来源为 {@link RolePolicy} */
    private Integer role;

    private String email;

    /** Spring Security 权限名（ROLE_ 前缀），全服务唯一映射口径 */
    public String getAuthority() {
        return RolePolicy.of(role).getAuthority();
    }

    /** 是否管理员（管理员 / 超级管理员），全服务唯一判定口径 */
    public boolean isAdmin() {
        return RolePolicy.isAdminCode(role);
    }
}
