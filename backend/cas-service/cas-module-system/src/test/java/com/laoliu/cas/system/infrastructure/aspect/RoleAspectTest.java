package com.laoliu.cas.system.infrastructure.aspect;

import com.laoliu.cas.common.api.GetUserIdViaTokenApi;
import com.laoliu.cas.common.annotation.RequireRole;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.exception.ForbiddenException;
import com.laoliu.cas.common.exception.UnauthorizedException;
import com.laoliu.cas.system.infrastructure.persistence.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RoleAspect 授权切面单元测试（7.3.1）。
 * <p>
 * 此前授权链路只有 cas-server 的纯反射守护测试（断言"注解存在"），注解写错或切面
 * 不生效时测试仍然全绿。此处用 {@link AspectJProxyFactory} 把 RoleAspect 真实织入
 * 代理，端到端验证 401/403/放行行为，不启动 Spring 上下文、不依赖中间件。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleAspect 授权切面")
class RoleAspectTest {

    private static final Long USER_ID = 100L;

    @Mock
    private GetUserIdViaTokenApi getUserIdViaTokenApi;
    @Mock
    private UserMapper userMapper;

    /** 方法级标注的目标 API */
    @SuppressWarnings("unused")
    static class MethodLevelApi {
        @RequireRole(UserRoleEnum.USER)
        public String userOnly() {
            return "ok";
        }

        @RequireRole(UserRoleEnum.ADMIN)
        public String adminOnly() {
            return "ok";
        }

        public String open() {
            return "ok";
        }
    }

    /** 仅类级标注：全部方法都要求 TEACHER */
    @RequireRole(UserRoleEnum.TEACHER)
    @SuppressWarnings("unused")
    static class ClassLevelApi {
        public String guarded() {
            return "ok";
        }
    }

    /** 类级 TEACHER，单个方法放宽为 USER——验证方法级优先 */
    @RequireRole(UserRoleEnum.TEACHER)
    @SuppressWarnings("unused")
    static class MixedApi {
        @RequireRole(UserRoleEnum.USER)
        public String relaxedByMethod() {
            return "ok";
        }

        public String stillTeacher() {
            return "ok";
        }
    }

    private <T> T proxyOf(T target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new RoleAspect(getUserIdViaTokenApi, userMapper));
        return factory.getProxy();
    }

    @Nested
    @DisplayName("身份与角色异常")
    class IdentityTests {

        @Test
        @DisplayName("未登录（userId 为 null）抛 UnauthorizedException，且不查角色")
        void shouldRejectWhenUserIdNull() {
            when(getUserIdViaTokenApi.getUserId()).thenReturn(null);
            MethodLevelApi api = proxyOf(new MethodLevelApi());

            assertThrows(UnauthorizedException.class, api::userOnly);
            verify(userMapper, never()).getRoleByUserId(USER_ID);
        }

        @Test
        @DisplayName("查库无角色（null）抛 ForbiddenException")
        void shouldRejectWhenRoleMissing() {
            when(getUserIdViaTokenApi.getUserId()).thenReturn(USER_ID);
            when(userMapper.getRoleByUserId(USER_ID)).thenReturn(null);
            MethodLevelApi api = proxyOf(new MethodLevelApi());

            assertThrows(ForbiddenException.class, api::userOnly);
        }

        @Test
        @DisplayName("角色编码为非数字脏数据时抛 ForbiddenException（7.3.6，此前为 NumberFormatException 500）")
        void shouldRejectWhenRoleNotNumeric() {
            when(getUserIdViaTokenApi.getUserId()).thenReturn(USER_ID);
            when(userMapper.getRoleByUserId(USER_ID)).thenReturn("teacher");
            MethodLevelApi api = proxyOf(new MethodLevelApi());

            ForbiddenException ex = assertThrows(ForbiddenException.class, api::userOnly);
            assertTrue(ex.getMessage().contains("角色数据异常"), "异常消息应说明角色数据异常，实际：" + ex.getMessage());
        }
    }

    @Nested
    @DisplayName("方法级 @RequireRole")
    class MethodLevelTests {

        @Test
        @DisplayName("角色匹配时放行")
        void shouldPassWhenRoleMatches() {
            when(getUserIdViaTokenApi.getUserId()).thenReturn(USER_ID);
            when(userMapper.getRoleByUserId(USER_ID)).thenReturn(String.valueOf(UserRoleEnum.USER.getCode()));
            MethodLevelApi api = proxyOf(new MethodLevelApi());

            assertEquals("ok", api.userOnly());
        }

        @Test
        @DisplayName("普通用户访问 ADMIN 接口抛 ForbiddenException")
        void shouldRejectWhenRoleInsufficient() {
            when(getUserIdViaTokenApi.getUserId()).thenReturn(USER_ID);
            when(userMapper.getRoleByUserId(USER_ID)).thenReturn(String.valueOf(UserRoleEnum.USER.getCode()));
            MethodLevelApi api = proxyOf(new MethodLevelApi());

            assertThrows(ForbiddenException.class, api::adminOnly);
        }

        @Test
        @DisplayName("超级管理员放行任意接口")
        void shouldLetSuperAdminThrough() {
            when(getUserIdViaTokenApi.getUserId()).thenReturn(USER_ID);
            when(userMapper.getRoleByUserId(USER_ID)).thenReturn(String.valueOf(UserRoleEnum.SUPER_ADMIN.getCode()));
            MethodLevelApi api = proxyOf(new MethodLevelApi());

            assertEquals("ok", api.adminOnly());
        }

        @Test
        @DisplayName("无 @RequireRole 的方法不查身份直接放行")
        void shouldBypassOpenMethod() {
            MethodLevelApi api = proxyOf(new MethodLevelApi());

            assertEquals("ok", api.open());
            verify(getUserIdViaTokenApi, never()).getUserId();
        }
    }

    @Nested
    @DisplayName("类级 @RequireRole（7.3.1 起真实生效）")
    class ClassLevelTests {

        @Test
        @DisplayName("满足类级角色时放行")
        void shouldPassClassLevel() {
            when(getUserIdViaTokenApi.getUserId()).thenReturn(USER_ID);
            when(userMapper.getRoleByUserId(USER_ID)).thenReturn(String.valueOf(UserRoleEnum.TEACHER.getCode()));
            ClassLevelApi api = proxyOf(new ClassLevelApi());

            assertEquals("ok", api.guarded());
        }

        @Test
        @DisplayName("不满足类级角色时抛 ForbiddenException")
        void shouldRejectClassLevel() {
            when(getUserIdViaTokenApi.getUserId()).thenReturn(USER_ID);
            when(userMapper.getRoleByUserId(USER_ID)).thenReturn(String.valueOf(UserRoleEnum.USER.getCode()));
            ClassLevelApi api = proxyOf(new ClassLevelApi());

            assertThrows(ForbiddenException.class, api::guarded);
        }

        @Test
        @DisplayName("方法级标注优先于类级：USER 可访问被方法放宽的端点")
        void methodAnnotationShouldOverrideClass() {
            when(getUserIdViaTokenApi.getUserId()).thenReturn(USER_ID);
            when(userMapper.getRoleByUserId(USER_ID)).thenReturn(String.valueOf(UserRoleEnum.USER.getCode()));
            MixedApi api = proxyOf(new MixedApi());

            assertEquals("ok", api.relaxedByMethod());
        }

        @Test
        @DisplayName("未被方法覆盖的端点仍执行类级角色：USER 被 TEACHER 类规则拒绝")
        void classLevelStillAppliesToUnannotatedMethod() {
            when(getUserIdViaTokenApi.getUserId()).thenReturn(USER_ID);
            when(userMapper.getRoleByUserId(USER_ID)).thenReturn(String.valueOf(UserRoleEnum.USER.getCode()));
            MixedApi api = proxyOf(new MixedApi());

            assertThrows(ForbiddenException.class, api::stillTeacher);
        }
    }
}
