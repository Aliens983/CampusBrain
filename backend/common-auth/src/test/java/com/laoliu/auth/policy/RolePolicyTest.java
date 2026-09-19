package com.laoliu.auth.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RolePolicy} 角色收敛策略单元测试。
 *
 * @author forever-king
 */
@DisplayName("全局角色策略 RolePolicy")
class RolePolicyTest {

    @Test
    @DisplayName("code 与权限名映射固定")
    void codesAndAuthoritiesAreStable() {
        assertEquals(0, RolePolicy.USER.getCode());
        assertEquals(1, RolePolicy.ADMIN.getCode());
        assertEquals(2, RolePolicy.SUPER_ADMIN.getCode());
        assertEquals(3, RolePolicy.TEACHER.getCode());
        assertEquals("ROLE_USER", RolePolicy.USER.getAuthority());
        assertEquals("ROLE_SUPER_ADMIN", RolePolicy.SUPER_ADMIN.getAuthority());
        assertEquals("ROLE_INTERNAL", RolePolicy.INTERNAL_AUTHORITY);
    }

    @Test
    @DisplayName("of：null 与未知 code 按最小权限降级为 USER，绝不抬权")
    void ofFallsBackToUser() {
        assertEquals(RolePolicy.USER, RolePolicy.of(null));
        assertEquals(RolePolicy.USER, RolePolicy.of(-1));
        assertEquals(RolePolicy.USER, RolePolicy.of(99));
        assertEquals(RolePolicy.TEACHER, RolePolicy.of(3));
    }

    @Test
    @DisplayName("ofHeader：空白/非法整数字符串降级 USER")
    void ofHeaderFallsBackToUser() {
        assertEquals(RolePolicy.USER, RolePolicy.ofHeader(null));
        assertEquals(RolePolicy.USER, RolePolicy.ofHeader("  "));
        assertEquals(RolePolicy.USER, RolePolicy.ofHeader("abc"));
        assertEquals(RolePolicy.ADMIN, RolePolicy.ofHeader("1"));
        assertEquals(RolePolicy.SUPER_ADMIN, RolePolicy.ofHeader(" 2 "));
    }

    @Test
    @DisplayName("isAdminCode：仅 1/2 为管理员")
    void adminCodeCheck() {
        assertTrue(RolePolicy.isAdminCode(1));
        assertTrue(RolePolicy.isAdminCode(2));
        assertFalse(RolePolicy.isAdminCode(0));
        assertFalse(RolePolicy.isAdminCode(3));
        assertFalse(RolePolicy.isAdminCode(null));
        assertFalse(RolePolicy.isAdminCode(-1));
    }

    @Test
    @DisplayName("isAssignableUserRole：仅 0/1/3 可经管理端分配；超管 2、null、越界均拒绝（防提权）")
    void assignableRoleCheck() {
        assertTrue(RolePolicy.isAssignableUserRole(0));
        assertTrue(RolePolicy.isAssignableUserRole(1));
        assertTrue(RolePolicy.isAssignableUserRole(3));
        assertFalse(RolePolicy.isAssignableUserRole(2));
        assertFalse(RolePolicy.isAssignableUserRole(null));
        assertFalse(RolePolicy.isAssignableUserRole(-1));
        assertFalse(RolePolicy.isAssignableUserRole(4));
    }

    @Test
    @DisplayName("内网身份不占数字 code：任何外部可解析的 code 都不会得到 ROLE_INTERNAL")
    void internalAuthorityIsNotReachableFromAnyCode() {
        for (int code = -10; code <= 10; code++) {
            assertNotEquals(RolePolicy.INTERNAL_AUTHORITY, RolePolicy.of(code).getAuthority());
        }
    }
}
