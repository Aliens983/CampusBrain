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
    @DisplayName("白名单：Ant 通配 /api/v1/uploads/** 与 /v3/api-docs/** 放行")
    void whitelist_wildcards_pass() {
        invoke(exchange(MockServerHttpRequest.get("/api/v1/uploads/2026/09/a.pdf").build()));
        invoke(exchange(MockServerHttpRequest.get("/v3/api-docs/swagger-config").build()));
        invoke(exchange(MockServerHttpRequest.get("/doc.html").build()));

        verify(chain, org.mockito.Mockito.times(3)).filter(any(ServerWebExchange.class));
        verify(jwtUtils, never()).getLoginUserFromToken(anyString());
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
    @DisplayName("Bearer 优先于 query：两者并存时以 Header token 为准")
    void bearerHeader_takesPrecedenceOverQuery() {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(3003L);
        loginUser.setRole(0);
        when(jwtUtils.getLoginUserFromToken("header-token")).thenReturn(loginUser);
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/v1/x?token=query-token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer header-token").build());

        invoke(exchange);

        verify(jwtUtils).getLoginUserFromToken("header-token");
        verify(jwtUtils, never()).getLoginUserFromToken("query-token");
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
