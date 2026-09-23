package com.laoliu.cas.system.interfaces.controller.admin;

import com.laoliu.cas.common.api.GetUserIdViaTokenApi;
import com.laoliu.cas.system.application.service.NotificationSettingsService;
import com.laoliu.cas.system.application.service.RoleService;
import com.laoliu.cas.system.application.service.UserService;
import com.laoliu.cas.system.domain.entity.User;
import com.laoliu.cas.system.domain.repository.UserRepository;
import com.laoliu.cas.system.interfaces.dto.request.AdminCreateUserRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理员建用户提权防护：超级管理员(2) 不能经接口创建；0/1/3 放行。
 * 判据与「管理员改角色」共用 RolePolicy#isAssignableUserRole。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
class AdminCreateUserRoleGuardTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private GetUserIdViaTokenApi getUserIdViaTokenApi;
    @Mock
    private UserService userService;
    @Mock
    private NotificationSettingsService notificationSettings;
    @Mock
    private RoleService roleService;

    @InjectMocks
    private UserController userController;

    @Test
    void superAdminRole_isRejected_andNotPersisted() {
        when(userRepository.getUserIdByEmail("super@campus.com")).thenReturn(null);

        var result = userController.createUser(buildRequest("super@campus.com", 2));

        assertThat(result.getMessage()).contains("超级管理员");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void teacherRole_isAllowed() {
        when(userRepository.getUserIdByEmail("teacher@campus.com")).thenReturn(null);

        var result = userController.createUser(buildRequest("teacher@campus.com", 3));

        assertThat(result.isSuccess()).isTrue();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void nullRole_defaultsToCommonUser() {
        when(userRepository.getUserIdByEmail("u@campus.com")).thenReturn(null);

        var result = userController.createUser(buildRequest("u@campus.com", null));

        assertThat(result.isSuccess()).isTrue();
        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(
                u -> u.getRole() != null && u.getRole() == 0));
    }

    private AdminCreateUserRequest buildRequest(String email, Integer role) {
        AdminCreateUserRequest req = new AdminCreateUserRequest();
        req.setName("张三");
        req.setEmail(email);
        req.setPassword("password123");
        req.setRole(role);
        return req;
    }
}
