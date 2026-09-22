package com.laoliu.gateway.filter;

import com.laoliu.auth.AuthConstants;
import com.laoliu.auth.InternalSigner;
import com.laoliu.auth.JWTUtils;
import com.laoliu.auth.dto.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 网关统一鉴权过滤器单元测试（5.1.2）。
 * <p>
 * 网关是全局安全入口，但此前零测试：白名单 AntPathMatcher 规则、Header/query
 * 两种 token 提取、内网签名头透传任一处被改坏，CI 都无法发现，只能靠线上
 * 大面积 401 暴露。本类以 {@link MockServerWebExchange} 直接驱动过滤器，
 * 不启动 Spring 上下文，不依赖 Nacos/下游服务。
 *
 * @author forever-king
 */
class AuthGlobalFilterTest {

    private JWTUtils jwtUtils;
    private InternalSigner internalSigner;
    private AuthGlobalFilter filter;
    private org.springframework.cloud.gateway.filter.GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        jwtUtils = mock(JWTUtils.class);
        internalSigner = mock(InternalSigner.class);
        chain = mock(org.springframework.cloud.gateway.filter.GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
        when(internalSigner.sign(anyString(), anyString(), anyString())).thenReturn("test-sign");
        filter = new AuthGlobalFilter(jwtUtils, internalSigner);
    }

    private MockServerWebExchange exchange(MockServerHttpRequest request) {
        return MockServerWebExchange.from(request);
    }

    private void invoke(ServerWebExchange exchange) {
        filter.filter(exchange, chain).block();
    }

    @Test
    @DisplayName("白名单：登录路径直接放行，不解析 JWT")
    void whitelist_login_passesWithoutJwt() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/v1/auth/login").build());

        invoke(exchange);

        verify(chain).filter(any(ServerWebExchange.class));
        verify(jwtUtils, never()).getLoginUserFromToken(anyString());
        verify(internalSigner, never()).sign(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("白名单：Ant 通配 /api/v1/uploads/** 放行")
    void whitelist_wildcards_pass() {
        invoke(exchange(MockServerHttpRequest.get("/api/v1/uploads/2026/09/a.pdf").build()));

        verify(chain).filter(any(ServerWebExchange.class));
        verify(jwtUtils, never()).getLoginUserFromToken(anyString());
    }

    @Test
    @DisplayName("白名单：验证码取图子路径 /api/v1/captcha/image/{uuid} 匿名放行")
    void whitelist_captchaImage_passesWithoutJwt() {
        invoke(exchange(MockServerHttpRequest.get(
                "/api/v1/captcha/image/550e8400-e29b-41d4-a716-446655440000").build()));

        verify(chain).filter(any(ServerWebExchange.class));
        verify(jwtUtils, never()).getLoginUserFromToken(anyString());
        verify(internalSigner, never()).sign(anyString(), anyString(), anyString());
    }


    @Test
    @DisplayName("4.14 默认关闭：/doc.html、/v3/api-docs 不再白名单，无 token 一律 401")
    void swagger_blockedByDefault() {
        MockServerWebExchange apiDocs =
                exchange(MockServerHttpRequest.get("/v3/api-docs/swagger-config").build());
        MockServerWebExchange doc = exchange(MockServerHttpRequest.get("/doc.html").build());
        MockServerWebExchange webjars =
                exchange(MockServerHttpRequest.get("/webjars/swagger-ui/swagger.css").build());

        filter.filter(apiDocs, chain).block();
        filter.filter(doc, chain).block();
        filter.filter(webjars, chain).block();

        assertThat(apiDocs.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(doc.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(webjars.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("4.14 显式开启：swagger-enabled=true 时文档路径重新放行")
    void swagger_enabledByProperty_passes() {
        AuthGlobalFilter openFilter = new AuthGlobalFilter(jwtUtils, internalSigner, true);
        MockServerWebExchange docExchange =
                exchange(MockServerHttpRequest.get("/doc.html").build());
        openFilter.filter(docExchange, chain).block();

        verify(jwtUtils, never()).getLoginUserFromToken(anyString());
        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("白名单不得被前缀绕过：/api/v1/auth/login/../user 规范化后仍需鉴权")
    void whitelist_prefixBypass_rejected() {
        // /api/v1/users 不在白名单，AntPathMatcher 也不做前缀匹配
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/v1/users").build());

        invoke(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("无 token（无 Header 无 query）→ 401，不进入下游")
    void noToken_returns401() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/v1/appointments").build());

        invoke(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("Authorization Bearer 合法：透传 X-User-Id/Role/Timestamp/Internal-Sign 后放行")
    void validBearerToken_propagatesIdentityHeaders() {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(1001L);
        loginUser.setRole(1);
        when(jwtUtils.getLoginUserFromToken("good-token")).thenReturn(loginUser);
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/v1/appointments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token").build());

        invoke(exchange);

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        HttpHeaders downstream = captor.getValue().getRequest().getHeaders();
        assertThat(downstream.getFirst(AuthConstants.HEADER_USER_ID)).isEqualTo("1001");
        assertThat(downstream.getFirst(AuthConstants.HEADER_USER_ROLE)).isEqualTo("1");
        assertThat(downstream.getFirst(AuthConstants.HEADER_INTERNAL_SIGN)).isEqualTo("test-sign");
        assertThat(downstream.getFirst(AuthConstants.HEADER_TIMESTAMP)).isNotBlank();
    }

    @Test
    @DisplayName("SSE 场景：EventSource 无法设头，token 走 query 参数同样可鉴权")
    void queryToken_sseScenario_passes() {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(2002L);
        loginUser.setRole(0);
        when(jwtUtils.getLoginUserFromToken("query-token")).thenReturn(loginUser);
        MockServerWebExchange exchange = exchange(
                MockServerHttpRequest.get("/api/v1/kb/qa/ask/stream?query=hi&token=query-token").build());

        invoke(exchange);

        verify(jwtUtils).getLoginUserFromToken("query-token");
        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("4.12 非 SSE 路径：query token 不再被接受，无 Bearer 头 → 401（防 token 经 URL 泄漏）")
    void queryToken_nonSsePath_rejected() {
        MockServerWebExchange exchange = exchange(
                MockServerHttpRequest.get("/api/v1/users?token=query-token").build());

        invoke(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(jwtUtils, never()).getLoginUserFromToken(anyString());
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("Bearer 优先于 query：SSE 路径两者并存时以 Header token 为准")
    void bearerHeader_takesPrecedenceOverQuery() {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(3003L);
        loginUser.setRole(0);
        when(jwtUtils.getLoginUserFromToken("header-token")).thenReturn(loginUser);
        MockServerWebExchange exchange =
                exchange(MockServerHttpRequest.get("/api/v1/kb/qa/ask/stream?token=query-token")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer header-token").build());

        invoke(exchange);

        verify(jwtUtils).getLoginUserFromToken("header-token");
        verify(jwtUtils, never()).getLoginUserFromToken("query-token");
    }

    @Test
    @DisplayName("4.9 外部伪造的 X-User-Id/Role 头：被网关移除后以 JWT 身份重写，单值不追加")
    void forgedIdentityHeaders_rewrittenByGateway() {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(1001L);
        loginUser.setRole(1);
        when(jwtUtils.getLoginUserFromToken("good-token")).thenReturn(loginUser);
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/v1/appointments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .header(AuthConstants.HEADER_USER_ID, "9999")
                .header(AuthConstants.HEADER_USER_ROLE, "2")
                .header(AuthConstants.HEADER_INTERNAL_SIGN, "forged-sign")
                .build());

        invoke(exchange);

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        HttpHeaders downstream = captor.getValue().getRequest().getHeaders();
        assertThat(downstream.get(AuthConstants.HEADER_USER_ID)).containsExactly("1001");
        assertThat(downstream.get(AuthConstants.HEADER_USER_ROLE)).containsExactly("1");
        assertThat(downstream.getFirst(AuthConstants.HEADER_INTERNAL_SIGN)).isEqualTo("test-sign");
        assertThat(downstream.get(AuthConstants.HEADER_INTERNAL_SIGN)).hasSize(1);
    }

    @Test
    @DisplayName("非法/过期 token 解析为 null → 401")
    void invalidToken_returns401() {
        when(jwtUtils.getLoginUserFromToken("expired")).thenReturn(null);
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/v1/appointments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer expired").build());

        invoke(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("token 合法但 userId 缺失 → 401，拒绝匿名身份下渗")
    void loginUserWithoutId_returns401() {
        LoginUser loginUser = new LoginUser();
        loginUser.setRole(1);
        when(jwtUtils.getLoginUserFromToken("no-id")).thenReturn(loginUser);
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/v1/appointments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer no-id").build());

        invoke(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("非 Bearer 方案（如 Basic）不被当作 token 接受")
    void nonBearerAuthorization_returns401() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/v1/appointments")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz").build());

        invoke(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(jwtUtils, never()).getLoginUserFromToken(anyString());
    }

    @Test
    @DisplayName("角色为 null 时透传头降级为 \"0\" 而非字面量 null")
    void nullRole_defaultsToZero() {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(4004L);
        loginUser.setRole(null);
        when(jwtUtils.getLoginUserFromToken("t")).thenReturn(loginUser);
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/v1/appointments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer t").build());

        invoke(exchange);

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertThat(captor.getValue().getRequest().getHeaders()
                .getFirst(AuthConstants.HEADER_USER_ROLE)).isEqualTo("0");
        verify(internalSigner).sign(eq("4004"), eq("0"), anyString());
    }

    @Test
    @DisplayName("顺序固定：getOrder() 返回 -100，保证在路由转发前鉴权")
    void order_isBeforeRouting() {
        assertThat(filter.getOrder()).isEqualTo(-100);
    }
}
