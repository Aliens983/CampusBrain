package com.laoliu.cas.common.annotation;

import com.laoliu.cas.common.enums.UserRoleEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 授权注解：可标注在 Controller 方法上（单端点），也可标注在类上（该类全部 HTTP 方法统一生效）。
 * 方法级标注优先于类级（7.3.1 起类级标注真正被 RoleAspect 读取，此前仅方法级生效）。
 *
 * @author forever-king
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    UserRoleEnum[] value() default {UserRoleEnum.USER};
}
