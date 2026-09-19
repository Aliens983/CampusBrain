package com.laoliu.cas.common.enums;

import com.laoliu.auth.policy.RolePolicy;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * CAS 用户角色枚举（展示与业务侧使用）。
 * <p>
 * 2.4 收敛：数字 code 的唯一来源是 {@link RolePolicy}（common-auth，gateway/cas/kb 共用），
 * 本枚举不再自维护 0/1/2/3，只补充 CAS 侧的中文描述。类加载时静态校验两边条目与 code
 * 完全对齐——任何人只改其中一处都会快速失败。
 *
 * @author forever-king
 */
@Getter
@AllArgsConstructor
public enum UserRoleEnum {
    USER(RolePolicy.USER.getCode(), "普通用户"),
    ADMIN(RolePolicy.ADMIN.getCode(), "管理员"),
    SUPER_ADMIN(RolePolicy.SUPER_ADMIN.getCode(), "超级管理员"),
    TEACHER(RolePolicy.TEACHER.getCode(), "教师");

    private final int code;
    private final String description;

    static {
        // 防漂移守卫：RolePolicy 新增/调整角色而本枚举未同步时，类加载即失败
        for (RolePolicy policy : RolePolicy.values()) {
            UserRoleEnum mapped;
            try {
                mapped = UserRoleEnum.valueOf(policy.name());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "UserRoleEnum 缺少 RolePolicy 角色映射: " + policy.name(), e);
            }
            if (mapped.code != policy.getCode()) {
                throw new IllegalStateException(
                        "UserRoleEnum 与 RolePolicy 角色 code 不一致: " + policy.name());
            }
        }
    }

    /**
     * 数字 code 解析；完全委托 {@link RolePolicy#of(Integer)}，
     * null/非法 code 按最小权限原则降级为普通用户。
     */
    public static UserRoleEnum getByCode(int code) {
        return UserRoleEnum.valueOf(RolePolicy.of(code).name());
    }

    /** 精确匹配授权（SUPER_ADMIN 需显式列出，等级不向上透传，避免 TEACHER 误越权） */
    public static boolean hasPermission(String userRole, UserRoleEnum... requiredRoles) {
        if (userRole == null) {
            return false;
        }
        final UserRoleEnum userRoleEnum;
        try {
            userRoleEnum = getByCode(Integer.parseInt(userRole));
        } catch (NumberFormatException e) {
            return false;
        }
        for (UserRoleEnum requiredRole : requiredRoles) {
            if (userRoleEnum == requiredRole) {
                return true;
            }
        }
        return false;
    }
}
