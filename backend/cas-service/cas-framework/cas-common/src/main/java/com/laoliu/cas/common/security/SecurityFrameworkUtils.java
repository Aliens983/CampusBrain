package com.laoliu.cas.common.security;

import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security 上下文工具类（无业务语义，跨模块复用）。
 *
 * @author forever-king
 */
public class SecurityFrameworkUtils {

    private SecurityFrameworkUtils() {
    }

    @Nullable
    public static Authentication getAuthentication() {
        SecurityContext context = SecurityContextHolder.getContext();
        return context == null ? null : context.getAuthentication();
    }

    @Nullable
    public static LoginUser getLoginUser() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    @Nullable
    public static Long getLoginUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getId() : null;
    }

    @Nullable
    public static String getLoginUserName() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getName() : null;
    }

    @Nullable
    public static Integer getLoginUserRole() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getRole() : null;
    }

    @Nullable
    public static String getLoginUserEmail() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getEmail() : null;
    }

    public static boolean isAdmin() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null && loginUser.isAdmin();
    }

    @Nullable
    public static Collection<? extends GrantedAuthority> getAuthorities() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return Collections.emptyList();
        }
        return authentication.getAuthorities();
    }

    public static boolean isAuthenticated() {
        return getLoginUser() != null;
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
