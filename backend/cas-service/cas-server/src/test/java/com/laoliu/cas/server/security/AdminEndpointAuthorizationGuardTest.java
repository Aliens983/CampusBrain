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
 * 授权守护测试（2.3.2）：
 * <p>
 * 安全框架层已移除 {@code /admin/**} 的粗粒度角色规则，授权唯一来源是
 * {@link RequireRole}（由 RoleAspect 实时查库判定）。为防止后续新增管理端
 * 接口时漏标注解导致「仅登录即可访问」，此处扫描所有控制器并断言：
 * 类级路径以 {@code /admin} 开头的控制器，其每个 HTTP 映射方法都必须显式
 * 标注 {@code @RequireRole}（或在类级统一标注）。
 * <p>
 * 纯反射实现，不启动 Spring 上下文、不依赖中间件。
 *
 * @author forever-king
 */
class AdminEndpointAuthorizationGuardTest {

    private static final String SCAN_BASE_PACKAGE = "com.laoliu.cas";
    private static final String ADMIN_PATH_PREFIX = "/admin";

    @Test
    @DisplayName("所有 /admin/** 端点必须显式声明 @RequireRole")
    void allAdminEndpointsMustDeclareRequireRole() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<String> violations = new ArrayList<>();
        int adminControllerCount = 0;

        for (BeanDefinition beanDefinition : scanner.findCandidateComponents(SCAN_BASE_PACKAGE)) {
            Class<?> controllerClass = Class.forName(beanDefinition.getBeanClassName());
            if (!hasAdminClassPath(controllerClass)) {
                continue;
            }
            adminControllerCount++;
            // 允许在类级统一标注 @RequireRole 对全部方法生效
            boolean classLevelGuarded = controllerClass.isAnnotationPresent(RequireRole.class);

            for (Method method : controllerClass.getDeclaredMethods()) {
                if (isHttpEndpoint(method) && !classLevelGuarded
                        && !method.isAnnotationPresent(RequireRole.class)) {
                    violations.add(controllerClass.getName() + "#" + method.getName());
                }
            }
        }

        // 防止扫描范围失效导致用例空转通过
        assertTrue(adminControllerCount >= 5,
                "守护测试未扫描到足够的管理端控制器，扫描可能失效，实际：" + adminControllerCount);
        assertTrue(violations.isEmpty(),
                "以下 /admin/** 端点缺少 @RequireRole 授权注解（2.3.2 后路径前缀不再赋予任何角色）：\n  - "
                        + String.join("\n  - ", violations));
    }

    private boolean hasAdminClassPath(Class<?> controllerClass) {
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
