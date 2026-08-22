package com.laoliu.cas.system.application.service.impl;

import com.laoliu.cas.common.exception.ForbiddenException;
import com.laoliu.cas.common.exception.ResourceNotFoundException;
import com.laoliu.cas.system.application.service.RoleService;
import com.laoliu.cas.system.domain.entity.User;
import com.laoliu.cas.system.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RoleServiceImpl 单元测试。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("角色服务单元测试")
class RoleServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private RoleService roleService;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        roleService = new RoleServiceImpl(userRepository);
    }

    // ======================== getRoleByUserId ========================

    @Nested
    @DisplayName("获取用户角色 - getRoleByUserId")
    class GetRoleByUserIdTests {

        @Test
        @DisplayName("role=0 时应当返回\"普通用户\"")
        void shouldReturnCommonUserWhenRoleIs0() {
            when(userRepository.getRoleByUserId(USER_ID)).thenReturn("0");
            assertEquals("普通用户", roleService.getRoleByUserId(USER_ID));
        }

        @Test
        @DisplayName("role=1 时应当返回\"管理员\"")
        void shouldReturnAdminWhenRoleIs1() {
            when(userRepository.getRoleByUserId(USER_ID)).thenReturn("1");
            assertEquals("管理员", roleService.getRoleByUserId(USER_ID));
        }

        @Test
        @DisplayName("role 为 null 时应当返回 null")
        void shouldReturnNullWhenRoleIsNull() {
            when(userRepository.getRoleByUserId(USER_ID)).thenReturn(null);
            assertNull(roleService.getRoleByUserId(USER_ID));
        }
    }

    // ======================== setRoleById ========================

    @Nested
    @DisplayName("设置用户角色 - setRoleById")
    class SetRoleByIdTests {

        @Test
        @DisplayName("newRole=1 时应当设置为管理员")
        void shouldSetToAdminWhenNewRoleIs1() {
            User user = User.builder().id(USER_ID).role(0).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            roleService.setRoleById(USER_ID, 1);

            verify(userRepository).updateRoleToAdmin(USER_ID);
            verify(userRepository, never()).updateRoleToCommonUser(anyLong());
        }

        @Test
        @DisplayName("newRole=0 时应当设置为普通用户")
        void shouldSetToCommonUserWhenNewRoleIs0() {
            User user = User.builder().id(USER_ID).role(1).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            roleService.setRoleById(USER_ID, 0);

            verify(userRepository).updateRoleToCommonUser(USER_ID);
            verify(userRepository, never()).updateRoleToAdmin(anyLong());
        }

        @Test
        @DisplayName("用户不存在时应当抛出 ResourceNotFoundException")
        void shouldThrowResourceNotFoundWhenUserNotExist() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> roleService.setRoleById(USER_ID, 1));

            verify(userRepository, never()).updateRoleToAdmin(anyLong());
            verify(userRepository, never()).updateRoleToCommonUser(anyLong());
        }

        @Test
        @DisplayName("试图修改超级管理员角色时应当抛出 ForbiddenException")
        void shouldThrowForbiddenWhenModifyingSuperAdmin() {
            User superAdmin = User.builder().id(USER_ID).role(2).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(superAdmin));

            assertThrows(ForbiddenException.class,
                    () -> roleService.setRoleById(USER_ID, 0));

            verify(userRepository, never()).updateRoleToAdmin(anyLong());
            verify(userRepository, never()).updateRoleToCommonUser(anyLong());
        }
    }
}
