package com.laoliu.cas.security.filter;

import com.laoliu.auth.AuthConstants;
import com.laoliu.auth.InternalSigner;
import com.laoliu.auth.policy.RolePolicy;
import com.laoliu.auth.web.AuthErrorResponses;
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 内网签名校验过滤器
 *
 * <p>当请求携带 {@code X-Internal-Sign} 时，校验签名与时间戳新鲜度，
 * 防止绕过网关伪造身份或重放签名头。若未携带签名头（例如本地直连），
 * 则剥离所有身份头后交由 {@link JWTFilter} 处理 JWT。
 *
 * <p>4.10：无签名时不再裸放原请求——外部可直连 18080 伪造
 * {@code X-User-Id}/{@code X-User-Role}，必须用 wrapper 统一剥除。
 *
 * @author forever-king
 */
@Slf4j
@RequiredArgsConstructor
public class InternalAuthFilter extends OncePerRequestFilter {

    private static final long MAX_AGE_SECONDS = 300L;

    /** 仅允许由网关签名背书的身份相关请求头，大小写不敏感匹配 */
    private static final Set<String> IDENTITY_HEADERS;

    static {
        Set<String> headers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        headers.add(AuthConstants.HEADER_USER_ID);
        headers.add(AuthConstants.HEADER_USER_ROLE);
        headers.add(AuthConstants.HEADER_TIMESTAMP);
        headers.add(AuthConstants.HEADER_INTERNAL_SIGN);
        IDENTITY_HEADERS = Collections.unmodifiableSet(headers);
    }

    private final InternalSigner internalSigner;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String userId = request.getHeader(AuthConstants.HEADER_USER_ID);
        String role = request.getHeader(AuthConstants.HEADER_USER_ROLE);
        String timestamp = request.getHeader(AuthConstants.HEADER_TIMESTAMP);
        String sign = request.getHeader(AuthConstants.HEADER_INTERNAL_SIGN);

        // 未携带内网签名（直连场景）：身份头一律不可信，剥除后交给 JWTFilter，
        // 防止绕过网关直连 18080 用伪造 X-User-Id/X-User-Role 冒充任意身份（4.10）
        if (sign == null) {
            chain.doFilter(new IdentityHeaderStrippingRequestWrapper(request), response);
            return;
        }

        // 携带签名则必须校验通过
        if (userId == null
                || !internalSigner.verifyWithTimestamp(sign, MAX_AGE_SECONDS, userId, role, timestamp)) {
            log.warn("内网签名校验失败: path={}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            // Q-07：统一错误体（AuthErrorResponses）
            response.setContentType(AuthErrorResponses.JSON_CONTENT_TYPE);
            response.getWriter().write(AuthErrorResponses.unauthorized("无效的内网身份签名"));
            return;
        }

        // 校验通过：将内网服务身份写入 SecurityContext，使 Security 的 authenticated() 放行，
        // 同时避免与 JWTFilter 冲突（JWTFilter 无 token 时不会覆盖此认证）
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "internal-service",
                        null,
                        List.of(new SimpleGrantedAuthority(RolePolicy.INTERNAL_AUTHORITY)));
        authentication.setDetails(new WebAuthenticationDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        chain.doFilter(request, response);
    }

    /**
     * 剥除内网身份头的请求包装器（4.10）。
     * <p>无签名直连时，外部伪造的 X-User-Id/X-User-Role 等必须在下游（解析器、
     * 其他过滤器、Controller）看来完全不存在，而不只是覆盖为空字符串——
     * 因此 getHeader/getHeaders/getHeaderNames 三个入口都要过滤。
     */
    static class IdentityHeaderStrippingRequestWrapper extends jakarta.servlet.http.HttpServletRequestWrapper {

        IdentityHeaderStrippingRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getHeader(String name) {
            if (IDENTITY_HEADERS.contains(name)) {
                return null;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (IDENTITY_HEADERS.contains(name)) {
                return Collections.emptyEnumeration();
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Enumeration<String> names = super.getHeaderNames();
            if (names == null) {
                return Collections.emptyEnumeration();
            }
            List<String> filtered = Collections.list(names).stream()
                    .filter(name -> !IDENTITY_HEADERS.contains(name))
                    .toList();
            return Collections.enumeration(filtered);
        }
    }
}
