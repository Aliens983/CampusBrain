package com.laoliu.cas.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 下游服务 TraceId 过滤器（5.2.1）。
 * <p>
 * 与网关 {@code TraceIdGlobalFilter} 配套：网关为每个入站请求生成/校验
 * {@code X-Trace-Id} 并在转发头中透传，本过滤器读取该头写入 SLF4J MDC，
 * 使 CAS 全部日志可按 traceId 与网关、KB 三段对齐。
 * </p>
 * <p>
 * 直连绕过网关的请求（本地调试/容器内网探测）没有该头时自行生成，
 * 保证 MDC 不为空；同时不信任任意脏值，仅接受受限字符集。
 * </p>
 *
 * @author forever-king
 */
public class TraceIdFilter extends OncePerRequestFilter {

    /** 全链路统一的请求/响应头名称 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** 日志 MDC key，与网关/KB 及日志 pattern 保持一致 */
    public static final String MDC_KEY = "traceId";

    private static final Pattern VALID_TRACE_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String incoming = request.getHeader(TRACE_ID_HEADER);
        String traceId = (incoming != null && VALID_TRACE_ID.matcher(incoming).matches())
                ? incoming
                : UUID.randomUUID().toString().replace("-", "");
        try {
            MDC.put(MDC_KEY, traceId);
            response.setHeader(TRACE_ID_HEADER, traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
