package com.laoliu.cas.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author forever-king
 */
@Getter
@AllArgsConstructor
public enum UserRoleEnum {
    USER(0, "普通用户"),
    ADMIN(1, "管理员"),
    SUPER_ADMIN(2, "超级管理员");

    private final int code;
    private final String description;

    public static UserRoleEnum getByCode(int code) {
        for (UserRoleEnum role : values()) {
            if (role.code == code) {
                return role;
            }
        }
        return USER;
    }

    public static boolean hasPermission(String userRole, UserRoleEnum... requiredRoles) {
        if (userRole == null) {
            return false;
        }
        UserRoleEnum userRoleEnum = getByCode(Integer.parseInt(userRole));
        for (UserRoleEnum requiredRole : requiredRoles) {
            if (userRoleEnum.code >= requiredRole.code) {
                return true;
            }
        }
        return false;
    }
}
