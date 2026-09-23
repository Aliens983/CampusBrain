package com.kb.infrastructure.security;

import com.laoliu.auth.InternalSigner;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link TokenAuthenticationFilter} 公开路径单测。
 * <p>
 * 重点守护：独立管理端口上的只读 Actuator 路径（health/prometheus）必须免内网签名，
 * 否则 Prometheus 抓取与容器探活会被 401 拒绝；其余路径在无签名头时必须被拦截。
 *
 * @author forever-king
 */
class TokenAuthenticationFilterTest {

    private InternalSigner internalSigner;
    private TokenAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        internalSigner = mock(InternalSigner.class);
        filter = new TokenAuthenticationFilter(internalSigner);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void actuatorPrometheus_passesWithoutSignature() throws Exception {
        assertPasses("/actuator/prometheus");
    }

    @Test
    void actuatorLiveness_passesWithoutSignature() throws Exception {
        assertPasses("/actuator/health/liveness");
    }

    @Test
    void actuatorInfo_passesWithoutSignature() throws Exception {
        assertPasses("/actuator/info");
    }

    @Test
    void actuatorEnv_withoutSignature_isRejected() throws Exception {
        MockHttpServletResponse response = invokeWithoutHeaders("/actuator/env");

        // env 不在暴露/放行清单：必须被签名校验拦截，防止管理端点在业务链路上被白名单带开
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void businessPath_withoutSignature_isRejected() throws Exception {
        MockHttpServletResponse response = invokeWithoutHeaders("/documents");

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(internalSigner);
    }

    @Test
    void businessPath_withValidSignature_passesAndSetsContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/documents");
        request.setServletPath("/documents");
        request.addHeader("X-User-Id", "7");
        request.addHeader("X-User-Role", "0");
        request.addHeader("X-Timestamp", String.valueOf(System.currentTimeMillis()));
        request.addHeader("X-Internal-Sign", "valid-sign");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(internalSigner.verifyWithTimestamp(
                org.mockito.ArgumentMatchers.eq("valid-sign"),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.eq("7"),
                org.mockito.ArgumentMatchers.eq("0"),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(true);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private void assertPasses(String servletPath) throws Exception {
        MockHttpServletResponse response = invokeWithoutHeaders(servletPath);

        assertThat(response.getStatus()).isEqualTo(200);
        // 公开路径完全不应触发签名校验
        verify(internalSigner, never()).verifyWithTimestamp(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(String[].class));
    }

    private MockHttpServletResponse invokeWithoutHeaders(String servletPath) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", servletPath);
        request.setServletPath(servletPath);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        if (response.getStatus() == 200) {
            verify(chain).doFilter(request, response);
        } else {
            verify(chain, never()).doFilter(request, response);
        }
        return response;
    }
}
