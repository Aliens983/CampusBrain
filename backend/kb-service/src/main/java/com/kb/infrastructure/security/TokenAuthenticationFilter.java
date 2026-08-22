package com.kb.infrastructure.security;

import com.laoliu.auth.AuthConstants;
import com.laoliu.auth.InternalSigner;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 网关身份信任过滤器。
 *
 * <p>统一鉴权后，KB 不再自行签发/校验 Token，而是信任网关透传的身份头：
 * 先校验 {@code X-Internal-Sign}（防绕过网关直连伪造），再用
 * {@code X-User-Id}/{@code X-User-Role} 恢复登录态注入 SecurityContext。
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** 无需网关身份头的公开路径 */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/kb/health",
            "/kb/auth/**"
    );

    private final InternalSigner internalSigner;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        String userId = request.getHeader(AuthConstants.HEADER_USER_ID);
        String role = request.getHeader(AuthConstants.HEADER_USER_ROLE);
        String timestamp = request.getHeader(AuthConstants.HEADER_TIMESTAMP);
        String sign = request.getHeader(AuthConstants.HEADER_INTERNAL_SIGN);

        if (userId == null || userId.isEmpty()
                || !internalSigner.verifyWithTimestamp(sign, 300, userId, role, timestamp)) {
            writeUnauthorized(response, "无效的内网身份签名");
            return;
        }

        LoginUser loginUser = LoginUser.builder()
                .userId(Long.parseLong(userId))
                .username(userId)
                .role(toRoleName(role))
                .build();
        SecurityFrameworkUtils.setLoginUser(loginUser);

        chain.doFilter(request, response);
    }

    /** CAS 数字角色（0/1/2）映射到 KB 的 ADMIN/USER 两级角色 */
    private String toRoleName(String numericRole) {
        if ("1".equals(numericRole) || "2".equals(numericRole)) {
            return "ADMIN";
        }
        return "USER";
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
    }
}
