package com.kb.infrastructure.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 安全框架工具类
 * <p>
 * 提供 Token 提取和当前登录用户获取等静态方法。
 * </p>
 *
 * @author forever-king
 */
public class SecurityFrameworkUtils {

    private SecurityFrameworkUtils() {}

    /**
     * 从请求头或 URL 参数中提取 Token
     *
     * @param request HTTP 请求
     * @param headerName 请求头名称（默认 Authorization）
     * @return Token 字符串（不含 Bearer 前缀），未找到返回 null
     */
    public static String obtainAuthorization(HttpServletRequest request, String headerName) {
        String token = request.getHeader(headerName);
        if (token == null || token.isEmpty()) {
            token = request.getParameter("token");
        }
        if (token == null || token.isEmpty()) {
            return null;
        }
        int index = token.indexOf("Bearer ");
        return index >= 0 ? token.substring(index + 7).trim() : token;
    }

    /**
     * 将 LoginUser 设置到 Spring Security 上下文
     */
    public static void setLoginUser(LoginUser loginUser) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 获取当前登录用户
     */
    public static LoginUser getLoginUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getLoginUserId() {
        LoginUser user = getLoginUser();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 清除当前上下文
     */
    public static void clearContext() {
        SecurityContextHolder.clearContext();
    }
}
