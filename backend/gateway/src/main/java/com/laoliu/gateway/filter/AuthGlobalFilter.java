package com.laoliu.gateway.filter;

import com.laoliu.auth.AuthConstants;
import com.laoliu.auth.InternalSigner;
import com.laoliu.auth.JWTUtils;
import com.laoliu.auth.dto.LoginUser;
import com.laoliu.auth.web.AuthErrorResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 网关统一鉴权过滤器（5.1.2 建立，4.12/4.14 加固）
 *
 * <p>核心职责：
 * <ol>
 *   <li>白名单路径直接放行（登录、注册、验证码、上传文件等）；</li>
 *   <li>其余请求解析 JWT，并向下游透传身份头（{@code X-User-Id}、{@code X-User-Role}）
 *       及内网签名头（{@code X-Internal-Sign}、{@code X-Timestamp}），
 *       下游 CAS 服务通过签名头信任网关转发的身份；</li>
 *   <li>非法/过期 token 返回 401。</li>
 * </ol>
 *
 * <p><b>4.12</b>：query 参数 {@code token} 仅用于 EventSource 无法设置请求头的
 * SSE 场景。此前对白名单外所有路径生效，导致 token 经 URL 泄漏（访问日志、
 * 浏览器历史、Referer、代理缓存），现限定为 SSE 流式路径（见 SSE_QUERY_TOKEN_PATHS）。
 *
 * <p><b>4.14</b>：Swagger/Knife4j 文档默认不再放行，由
 * {@code gateway.security.swagger-enabled}（默认 false）显式开启，
 * 避免接口结构在生产环境裸奔。
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** query 参数方式携带 token 仅允许的 SSE 路径（4.12：kb 流式问答唯一端点） */
    private static final List<String> SSE_QUERY_TOKEN_PATHS = List.of("/api/v1/kb/**/stream");

    /** 与鉴权无关的公开路径（精确/通配） */
    private static final List<String> PUBLIC_WHITELIST = List.of(
            // 认证相关
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/verification-code",
            "/api/v1/auth/reset",
            "/api/v1/captcha",
            // KB 健康探针
            "/api/v1/kb/health",
            // 上传文件：UUID 文件名不可枚举，由下游应用层控制访问
            "/api/v1/uploads/**"
    );

    /** Swagger/Knife4j 文档路径（4.14：仅在显式开启时放行） */
    private static final List<String> SWAGGER_WHITELIST = List.of(
            "/doc.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**"
    );

    private final JWTUtils jwtUtils;
    private final InternalSigner internalSigner;
    /** 4.14：生产默认关闭接口文档白名单，开启需显式配置 gateway.security.swagger-enabled=true */
    private final List<String> whitelist;

    public AuthGlobalFilter(JWTUtils jwtUtils, InternalSigner internalSigner) {
        this(jwtUtils, internalSigner, false);
    }

    @Autowired
    public AuthGlobalFilter(JWTUtils jwtUtils,
                            InternalSigner internalSigner,
                            @Value("${gateway.security.swagger-enabled:false}") boolean swaggerEnabled) {
        this.jwtUtils = jwtUtils;
        this.internalSigner = internalSigner;
        List<String> paths = new ArrayList<>(PUBLIC_WHITELIST);
        if (swaggerEnabled) {
            paths.addAll(SWAGGER_WHITELIST);
        }
        this.whitelist = List.copyOf(paths);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 白名单放行
        if (isWhitelistedPath(path)) {
            return chain.filter(exchange);
        }

        // 2. 提取并校验 token
        String token = extractToken(request, path);
        if (token == null || token.isBlank()) {
            return unauthorized(exchange, "缺少认证令牌");
        }

        LoginUser loginUser;
        try {
            loginUser = jwtUtils.getLoginUserFromToken(token);
        } catch (Exception e) {
            log.warn("JWT 解析异常: path={}, error={}", path, e.getMessage());
            return unauthorized(exchange, "认证令牌无效");
        }

        if (loginUser == null || loginUser.getId() == null) {
            return unauthorized(exchange, "认证令牌无效或已过期");
        }

        // 3. 透传用户身份 + 内网签名头，下游凭签名信任网关转发
        String userId = String.valueOf(loginUser.getId());
        String role = loginUser.getRole() == null ? "0" : String.valueOf(loginUser.getRole());
        String timestamp = String.valueOf(System.currentTimeMillis());
        String sign = internalSigner.sign(userId, role, timestamp);

        // 4.9 纵深防御：先移除外部请求可能自带的伪造身份头，再写入网关背书的头，
        // 避免 HttpHeaders 追加语义导致下游读到多值（第一个值为伪造值）
        ServerHttpRequest mutated = request.mutate()
                .headers(headers -> {
                    headers.remove(AuthConstants.HEADER_USER_ID);
                    headers.remove(AuthConstants.HEADER_USER_ROLE);
                    headers.remove(AuthConstants.HEADER_TIMESTAMP);
                    headers.remove(AuthConstants.HEADER_INTERNAL_SIGN);
                })
                .header(AuthConstants.HEADER_USER_ID, userId)
                .header(AuthConstants.HEADER_USER_ROLE, role)
                .header(AuthConstants.HEADER_TIMESTAMP, timestamp)
                .header(AuthConstants.HEADER_INTERNAL_SIGN, sign)
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isWhitelistedPath(String path) {
        for (String pattern : whitelist) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSsePath(String path) {
        for (String pattern : SSE_QUERY_TOKEN_PATHS) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 提取认证令牌：优先 Authorization Bearer 头；
     * query 参数 {@code token} 仅限 SSE 路径（4.12：EventSource 无法设置请求头），
     * 其他路径忽略 query token，避免令牌经 URL 泄漏。
     */
    private String extractToken(ServerHttpRequest request, String path) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (isSsePath(path)) {
            return request.getQueryParams().getFirst("token");
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        // Q-07：错误体统一走 common-auth AuthErrorResponses（code/message/timestamp），
        // 与 kb ApiResponse / cas CommonResult 同口径
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, AuthErrorResponses.JSON_CONTENT_TYPE);
        byte[] bytes = AuthErrorResponses.unauthorized(message)
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
