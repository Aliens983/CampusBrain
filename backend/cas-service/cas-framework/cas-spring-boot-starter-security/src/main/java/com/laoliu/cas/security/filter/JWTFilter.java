package com.laoliu.cas.security.filter;

import com.laoliu.cas.common.security.JWTUtils;
import com.laoliu.cas.common.security.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * @author forever-king
 */
@Slf4j
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtils jwtUtils;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            try {
                LoginUser loginUser = jwtUtils.getLoginUserFromToken(token);
                if (loginUser != null) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            loginUser,
                            null,
                            List.of(new SimpleGrantedAuthority(resolveAuthority(loginUser.getRole())))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("用户 {} 认证成功", loginUser.getId());
                }
            } catch (Exception e) {
                log.error("JWT authentication failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /** 数字角色映射为 Spring Security 权限（ROLE_ 前缀） */
    private String resolveAuthority(Integer role) {
        if (role == null) {
            return "ROLE_USER";
        }
        return switch (role) {
            case 1 -> "ROLE_ADMIN";
            case 2 -> "ROLE_SUPER_ADMIN";
            default -> "ROLE_USER";
        };
    }
}