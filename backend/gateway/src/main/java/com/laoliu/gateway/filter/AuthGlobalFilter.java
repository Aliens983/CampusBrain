package com.laoliu.gateway.filter;

import com.laoliu.auth.AuthConstants;
import com.laoliu.auth.InternalSigner;
import com.laoliu.auth.JWTUtils;
import com.laoliu.auth.dto.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关统一鉴权过滤器
 *
 * <p>职责：验签统一 JWT，未登录拦截；把用户身份与内网签名透传给下游服务
 * 细粒度授权仍由各服务的 {@code @RequireRole} 负责（防御纵深）
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** 无需登录即可访问的路径 */
    private static final List<String> WHITELIST = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/verification-code",
            "/api/v1/auth/reset",
            "/api/v1/captcha",
            "/api/v1/kb/health",
            "/api/v1/uploads/**",
            "/doc.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**"
    );

    private final JWTUtils jwtUtils;
    private final InternalSigner internalSigner;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange.getRequest());
        if (token == null) {
            return unauthorized(exchange);
        }

        LoginUser loginUser = jwtUtils.getLoginUserFromToken(token);
        if (loginUser == null || loginUser.getId() == null) {
            return unauthorized(exchange);
        }

        String userId = String.valueOf(loginUser.getId());
        String role = loginUser.getRole() == null ? "0" : String.valueOf(loginUser.getRole());
        String timestamp = String.valueOf(System.currentTimeMillis());
        String sign = internalSigner.sign(userId, role, timestamp);

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(AuthConstants.HEADER_USER_ID, userId)
                .header(AuthConstants.HEADER_USER_ROLE, role)
                .header(AuthConstants.HEADER_TIMESTAMP, timestamp)
                .header(AuthConstants.HEADER_INTERNAL_SIGN, sign)
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        // 在路由转发前执行
        return -100;
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private String extractToken(ServerHttpRequest request) {
        String bearer = request.getHeaders().getFirst("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        // EventSource 无法设置 Authorization 头，退化为从 query 参数读取 token（仅用于 SSE 场景）
        String token = request.getQueryParams().getFirst("token");
        if (token != null && !token.isBlank()) {
            return token;
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = "{\"code\":401,\"message\":\"未登录或登录已过期\"}"
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
