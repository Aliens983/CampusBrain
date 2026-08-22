package com.laoliu.cas.common.annotation;

import com.laoliu.cas.common.enums.UserRoleEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author forever-king
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    UserRoleEnum[] value() default {UserRoleEnum.USER};
}
