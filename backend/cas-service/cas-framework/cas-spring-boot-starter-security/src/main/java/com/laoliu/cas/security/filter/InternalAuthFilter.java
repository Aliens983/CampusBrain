package com.laoliu.cas.security.filter;

import com.laoliu.auth.AuthConstants;
import com.laoliu.auth.InternalSigner;
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
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 内网签名校验过滤器
 *
 * <p>当请求携带 {@code X-Internal-Sign} 时，校验签名与时间戳新鲜度，
 * 防止绕过网关伪造身份或重放签名头。若未携带签名头（例如本地直连），
 * 则交由 {@link JWTFilter} 处理 JWT
 *
 * @author forever-king
 */
@Slf4j
@RequiredArgsConstructor
public class InternalAuthFilter extends OncePerRequestFilter {

    private static final long MAX_AGE_SECONDS = 300L;

    private final InternalSigner internalSigner;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String userId = request.getHeader(AuthConstants.HEADER_USER_ID);
        String role = request.getHeader(AuthConstants.HEADER_USER_ROLE);
        String timestamp = request.getHeader(AuthConstants.HEADER_TIMESTAMP);
        String sign = request.getHeader(AuthConstants.HEADER_INTERNAL_SIGN);

        // 未携带内网签名（直连场景），跳过内网校验，交给 JWTFilter
        if (sign == null) {
            chain.doFilter(request, response);
            return;
        }

        // 携带签名则必须校验通过
        if (userId == null
                || !internalSigner.verifyWithTimestamp(sign, MAX_AGE_SECONDS, userId, role, timestamp)) {
            log.warn("内网签名校验失败: path={}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"无效的内网身份签名\"}");
            return;
        }

        // 校验通过：将内网服务身份写入 SecurityContext，使 Security 的 authenticated() 放行，
        // 同时避免与 JWTFilter 冲突（JWTFilter 无 token 时不会覆盖此认证）
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "internal-service",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_INTERNAL")));
        authentication.setDetails(new WebAuthenticationDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        chain.doFilter(request, response);
    }
}
