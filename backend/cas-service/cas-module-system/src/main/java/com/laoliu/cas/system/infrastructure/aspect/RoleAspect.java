package com.laoliu.cas.system.infrastructure.aspect;

import com.laoliu.auth.policy.RolePolicy;
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
 * 角色权限校验切面
 * <p>
 * 拦截方法级或类级标注 {@link RequireRole} 的端点（方法级优先），校验当前用户是否具备所需角色；
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

    /**
     * 方法级注解（@annotation）或类级注解（@within）均触发；Spring AOP 代理只拦 public 方法（7.3.1）。
     */
    @Pointcut("@annotation(com.laoliu.cas.common.annotation.RequireRole) "
            + "|| @within(com.laoliu.cas.common.annotation.RequireRole)")
    public void requireRolePointcut() {
    }

    @Around("requireRolePointcut()")
    public Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 方法级标注优先；缺省时回退类级标注（7.3.1：类级 @RequireRole 此前因 @Target 不含 TYPE 且
        // 切面只读方法注解而成为死代码，守护测试的 classLevelGuarded 分支永远为 false）
        RequireRole requireRole = signature.getMethod().getAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = joinPoint.getTarget().getClass().getAnnotation(RequireRole.class);
        }

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

        // 角色列正常只会是数字编码；脏数据/手工改库可能写入非数字，
        // 此前 parseInt 直接抛 NumberFormatException → 500。此处收敛为 403（7.3.6）。
        int currentRoleCode;
        try {
            currentRoleCode = Integer.parseInt(userRole);
        } catch (NumberFormatException e) {
            log.warn("用户角色编码非数字，拒绝访问：userId={}, role={}", userId, userRole);
            throw new ForbiddenException(403, "用户角色数据异常，请联系管理员");
        }

        UserRoleEnum[] requiredRoles = requireRole.value();
        boolean hasPermission = hasPermission(currentRoleCode, requiredRoles);

        if (!hasPermission) {
            log.warn("用户权限不足，当前角色: {}, 需要角色: {}",
                    UserRoleEnum.getByCode(currentRoleCode).getDescription(),
                    Arrays.toString(requiredRoles));
            throw new ForbiddenException(403, "权限不足，无法访问该接口");
        }

        log.debug("权限验证通过，用户角色: {}", UserRoleEnum.getByCode(currentRoleCode).getDescription());
        return joinPoint.proceed();
    }

    private boolean hasPermission(int currentRoleCode, UserRoleEnum[] requiredRoles) {
        // 超级管理员拥有全部权限，直接放行（角色 code 口径唯一来源 RolePolicy）
        if (currentRoleCode == RolePolicy.SUPER_ADMIN.getCode()) {
            return true;
        }
        for (UserRoleEnum role : requiredRoles) {
            if (role.getCode() == currentRoleCode) {
                return true;
            }
            // 教师 = 登录用户的一种：凡开放给「普通用户 USER」的通用接口，教师同样可用（教师专属接口则显式列出 TEACHER）
            if (role == UserRoleEnum.USER && currentRoleCode == RolePolicy.TEACHER.getCode()) {
                return true;
            }
        }
        return false;
    }
}
