package com.kb.infrastructure.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 为每个 HTTP 请求注入 TraceId 到 MDC，实现全链路日志追踪
 * <p>
 * 优先级最高，确保后续所有日志都带有 TraceId
 * </p>
 * @author forever-king
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    /** TraceId Header 名称，支持前端传入 */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    /** MDC Key */
    private static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 优先使用前端传入的 TraceId，否则自动生成
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        try {
            MDC.put(MDC_KEY, traceId);
            // 把 TraceId 返回给前端，方便排查
            response.addHeader(TRACE_ID_HEADER, traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
