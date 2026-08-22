package com.laoliu.cas.system.infrastructure.aspect;

import com.laoliu.cas.common.annotation.RequireRole;
import com.laoliu.cas.common.api.GetUserIdViaTokenApi;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.exception.ForbiddenException;
import com.laoliu.cas.common.exception.UnauthorizedException;
import com.laoliu.cas.system.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 角色权限校验切面。
 * <p>
 * 拦截所有标注 {@link RequireRole} 的方法，校验当前用户是否具备所需角色。
 * 权限不足时直接抛出异常，由 {@code GlobalExceptionHandler} 统一处理。
 *
 * @author forever-king
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RoleAspect {

    private final GetUserIdViaTokenApi getUserIdViaTokenApi;
    private final UserMapper userMapper;

    @Pointcut("@annotation(com.laoliu.cas.common.annotation.RequireRole)")
    public void requireRolePointcut() {
    }

    @Around("requireRolePointcut()")
    public Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequireRole requireRole = signature.getMethod().getAnnotation(RequireRole.class);

        if (requireRole == null) {
            return joinPoint.proceed();
        }

        Long userId = getUserIdViaTokenApi.getUserId();
        if (userId == null) {
            throw new UnauthorizedException(401, "用户未登录");
        }

        String userRole = userMapper.getRoleByUserId(userId);
        if (userRole == null) {
            throw new ForbiddenException(403, "无法获取用户角色信息");
        }

        UserRoleEnum[] requiredRoles = requireRole.value();
        boolean hasPermission = hasPermission(userRole, requiredRoles);

        if (!hasPermission) {
            log.warn("用户权限不足，当前角色: {}, 需要角色: {}",
                    UserRoleEnum.getByCode(Integer.parseInt(userRole)).getDescription(),
                    Arrays.toString(requiredRoles));
            throw new ForbiddenException(403, "权限不足，无法访问该接口");
        }

        log.debug("权限验证通过，用户角色: {}", UserRoleEnum.getByCode(Integer.parseInt(userRole)).getDescription());
        return joinPoint.proceed();
    }

    private boolean hasPermission(String userRole, UserRoleEnum[] requiredRoles) {
        int currentRoleCode = Integer.parseInt(userRole);
        // 超级管理员拥有全部权限，直接放行
        if (currentRoleCode == UserRoleEnum.SUPER_ADMIN.getCode()) {
            return true;
        }
        for (UserRoleEnum role : requiredRoles) {
            if (role.getCode() == currentRoleCode) {
                return true;
            }
        }
        return false;
    }
}
