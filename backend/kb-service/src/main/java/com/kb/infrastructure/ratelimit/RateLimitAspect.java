package com.kb.infrastructure.ratelimit;

import com.kb.infrastructure.cache.CacheKeyBuilder;
import com.kb.infrastructure.common.BusinessException;
import com.kb.infrastructure.common.ErrorCode;
import com.kb.infrastructure.ratelimit.annotations.RateLimit;
import com.kb.infrastructure.security.SecurityFrameworkUtils;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 限流 AOP 切面
 * <p>
 * 拦截带 @RateLimit 注解的方法，调用 Redis Lua 脚本判断是否限流
 * </p>
 *
 * @author forever-king
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimiter rateLimiter;
    private final CacheKeyBuilder keyBuilder;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String userIdStr = userId != null ? String.valueOf(userId) : "anonymous";
        String key = keyBuilder.rateLimitKey(userIdStr, rateLimit.key());

        if (!rateLimiter.isAllowed(key, rateLimit.permits(), rateLimit.seconds())) {
            throw new BusinessException.RateLimitException(ErrorCode.RATE_LIMITED,
                    rateLimit.message());
        }
        return pjp.proceed();
    }
}
