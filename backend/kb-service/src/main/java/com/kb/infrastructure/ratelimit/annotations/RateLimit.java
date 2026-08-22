package com.kb.infrastructure.ratelimit.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 *
 * @author forever-king
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 限流标识，如 "qa:{userId}" */
    String key() default "";

    /** 时间窗口内允许的次数 */
    int permits() default 10;

    /** 时间窗口（秒） */
    int seconds() default 60;

    /** 超限提示信息 */
    String message() default "请求过于频繁，请稍后再试";
}
