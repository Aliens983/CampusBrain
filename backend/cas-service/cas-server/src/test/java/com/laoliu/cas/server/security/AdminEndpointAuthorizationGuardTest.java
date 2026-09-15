package com.laoliu.cas.server.security;

import com.laoliu.cas.common.annotation.RequireRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 授权守护测试（2.3.2 / 7.3.1）：
 * <p>
 * 安全框架层已移除 {@code /admin/**} 的粗粒度角色规则，授权唯一来源是
 * {@link RequireRole}（由 RoleAspect 实时查库判定）。为防止后续新增管理端
 * 接口时漏标注解导致「仅登录即可访问」，此处扫描所有控制器并断言：
 * 管理端控制器（位于 {@code .admin.} 包，或类级路径以 {@code /admin} 开头）
 * 的每个 HTTP 映射方法都必须显式标注 {@code @RequireRole}，或在类级统一标注。
 * <p>
 * 7.3.1 修正两点：① 包路径纳入判定——此前仅看路径前缀，漏掉了
 * {@code system...controller.admin.UserController}（其类路径为 {@code /users}）；
 * ② {@code @RequireRole} 的 @Target 已扩到 TYPE，类级标注为真实生效配置（由
 * RoleAspectTest 以织入代理实测），此处的类级分支不再是恒假死代码。
 * <p>
 * 纯反射实现，不启动 Spring 上下文、不依赖中间件。
 *
 * @author forever-king
 */
class AdminEndpointAuthorizationGuardTest {

    private static final String SCAN_BASE_PACKAGE = "com.laoliu.cas";
    private static final String ADMIN_PACKAGE_MARKER = ".admin.";
    private static final String ADMIN_PATH_PREFIX = "/admin";

    @Test
    @DisplayName("所有管理端端点（.admin. 包或 /admin 路径）必须显式声明 @RequireRole")
    void allAdminEndpointsMustDeclareRequireRole() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<String> violations = new ArrayList<>();
        int adminControllerCount = 0;

        for (BeanDefinition beanDefinition : scanner.findCandidateComponents(SCAN_BASE_PACKAGE)) {
            String className = beanDefinition.getBeanClassName();
            Class<?> controllerClass = Class.forName(className);
            if (!isAdminController(className, controllerClass)) {
                continue;
            }
            adminControllerCount++;
            // 允许在类级统一标注 @RequireRole 对全部方法生效（7.3.1 起由 RoleAspect 真实读取）
            boolean classLevelGuarded = controllerClass.isAnnotationPresent(RequireRole.class);

            for (Method method : controllerClass.getDeclaredMethods()) {
                if (isHttpEndpoint(method) && !classLevelGuarded
                        && !method.isAnnotationPresent(RequireRole.class)) {
                    violations.add(className + "#" + method.getName());
                }
            }
        }

        // 防止扫描范围失效导致用例空转通过（当前管理端控制器共 9 个：infra 2 / appointment 3 / system 4）
        assertTrue(adminControllerCount >= 8,
                "守护测试未扫描到足够的管理端控制器，扫描可能失效，实际：" + adminControllerCount);
        assertTrue(violations.isEmpty(),
                "以下管理端端点缺少 @RequireRole 授权注解（2.3.2 后路径前缀不再赋予任何角色）：\n  - "
                        + String.join("\n  - ", violations));
    }

    /**
     * 管理端控制器判定：包路径含 {@code .admin.}（如 UserController 虽类路径为 /users，
     * 但位于 system...controller.admin 包），或类级 @RequestMapping 以 /admin 开头（7.3.1）。
     */
    private boolean isAdminController(String className, Class<?> controllerClass) {
        if (className.contains(ADMIN_PACKAGE_MARKER)) {
            return true;
        }
        RequestMapping classMapping = controllerClass.getDeclaredAnnotation(RequestMapping.class);
        if (classMapping == null) {
            return false;
        }
        for (String path : classMapping.value()) {
            if (path.startsWith(ADMIN_PATH_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    private boolean isHttpEndpoint(Method method) {
        return method.isAnnotationPresent(GetMapping.class)
                || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class)
                || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(RequestMapping.class);
    }
}
