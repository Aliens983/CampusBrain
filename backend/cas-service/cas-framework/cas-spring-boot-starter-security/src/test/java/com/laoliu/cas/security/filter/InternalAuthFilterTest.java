package com.laoliu.cas.security.filter;

import com.laoliu.auth.AuthConstants;
import com.laoliu.auth.InternalSigner;
import jakarta.servlet.ServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * InternalAuthFilter 单元测试（4.10）。
 * <p>
 * 核心守护：无内网签名直连时，外部伪造的 X-User-Id/X-User-Role/X-Timestamp/
 * X-Internal-Sign 必须从下游视角完全消失——此前过滤器裸放原请求，绕过网关
 * 直连 18080 即可冒充任意身份。
 *
 * @author forever-king
 */
class InternalAuthFilterTest {

    private InternalSigner internalSigner;
    private InternalAuthFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private final AtomicReference<ServletRequest> downstreamRequest = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        internalSigner = mock(InternalSigner.class);
        filter = new InternalAuthFilter(internalSigner);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        downstreamRequest.set(null);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void invoke() throws Exception {
        filter.doFilter(request, response,
                (req, res) -> downstreamRequest.set(req));
    }

    @Test
    @DisplayName("4.10 无签名直连：伪造的身份头在下游请求中被完全剥除")
    void noSign_forgedIdentityHeaders_stripped() throws Exception {
        request.addHeader(AuthConstants.HEADER_USER_ID, "9999");
        request.addHeader(AuthConstants.HEADER_USER_ROLE, "2");
        request.addHeader(AuthConstants.HEADER_TIMESTAMP, "1");
        // 仅带伪造身份头但无 X-Internal-Sign
        request.addHeader("X-Trace-Id", "trace-1");

        invoke();

        ServletRequest downstream = downstreamRequest.get();
        assertTrue(downstream instanceof jakarta.servlet.http.HttpServletRequest);
        jakarta.servlet.http.HttpServletRequest wrapped =
                (jakarta.servlet.http.HttpServletRequest) downstream;
        assertNull(wrapped.getHeader(AuthConstants.HEADER_USER_ID));
        assertNull(wrapped.getHeader(AuthConstants.HEADER_USER_ROLE));
        assertNull(wrapped.getHeader(AuthConstants.HEADER_TIMESTAMP));
        assertFalse(wrapped.getHeaders(AuthConstants.HEADER_USER_ID).hasMoreElements());
        // 大小写变体同样剥除
        assertNull(wrapped.getHeader("x-user-id"));
        // 非身份头保留
        assertEquals("trace-1", wrapped.getHeader("X-Trace-Id"));
        // headerNames 枚举里也不得残留身份头
        boolean identityLeak = java.util.Collections.list(wrapped.getHeaderNames()).stream()
                .anyMatch(name -> name.equalsIgnoreCase(AuthConstants.HEADER_USER_ID)
                        || name.equalsIgnoreCase(AuthConstants.HEADER_USER_ROLE)
                        || name.equalsIgnoreCase(AuthConstants.HEADER_TIMESTAMP)
                        || name.equalsIgnoreCase(AuthConstants.HEADER_INTERNAL_SIGN));
        assertFalse(identityLeak);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("携带合法签名：身份头保留且写入 INTERNAL 认证")
    void validSign_passesAndAuthenticates() throws Exception {
        request.addHeader(AuthConstants.HEADER_USER_ID, "1001");
        request.addHeader(AuthConstants.HEADER_USER_ROLE, "0");
        request.addHeader(AuthConstants.HEADER_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        request.addHeader(AuthConstants.HEADER_INTERNAL_SIGN, "good-sign");
        when(internalSigner.verifyWithTimestamp(any(), anyLong(), any(), any(), any())).thenReturn(true);

        invoke();

        ServletRequest downstream = downstreamRequest.get();
        assertTrue(downstream instanceof jakarta.servlet.http.HttpServletRequest);
        jakarta.servlet.http.HttpServletRequest wrapped =
                (jakarta.servlet.http.HttpServletRequest) downstream;
        assertEquals("1001", wrapped.getHeader(AuthConstants.HEADER_USER_ID));
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_INTERNAL")));
    }

    @Test
    @DisplayName("携带非法签名：401 且不进入下游")
    void invalidSign_returns401() throws Exception {
        request.addHeader(AuthConstants.HEADER_USER_ID, "1001");
        request.addHeader(AuthConstants.HEADER_INTERNAL_SIGN, "bad-sign");
        when(internalSigner.verifyWithTimestamp(any(), anyLong(), any(), any(), any())).thenReturn(false);

        invoke();

        assertEquals(401, response.getStatus());
        assertNull(downstreamRequest.get());
    }
}
