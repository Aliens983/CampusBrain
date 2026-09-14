package com.laoliu.gateway.filter;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 全链路 TraceId 入口过滤器（5.2.1）。
 * <p>
 * 职责：
 * <ol>
 *   <li>请求进入网关时，优先沿用上游（如 nginx/客户端）传入的 {@code X-Trace-Id}，
 *       否则生成 32 位十六进制 ID；</li>
 *   <li>把 traceId 注入转发请求头，供 CAS/KB 下游 Filter 读入各自 MDC；</li>
 *   <li>回写响应头，方便前端/调用方报障时提供追踪 ID；</li>
 *   <li>网关自身日志在同步路由阶段可通过 MDC 打印 traceId
 *       （响应式线程切换后的异步日志不保证携带，跨服务追踪以请求头为准）。</li>
 * </ol>
 * 顺序高于 {@link AuthGlobalFilter}，确保鉴权失败的请求也有 traceId 可查。
 *
 * @author forever-king
 */
@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    /** 全链路统一的请求/响应头名称 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** 日志 MDC key，与下游服务及日志 pattern 保持一致 */
    public static final String MDC_KEY = "traceId";

    /** 只接受长度 8-64 的字母数字及中横，防止头注入脏值 */
    private static final Pattern VALID_TRACE_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        String traceId = (incoming != null && VALID_TRACE_ID.matcher(incoming).matches())
                ? incoming
                : UUID.randomUUID().toString().replace("-", "");

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> headers.set(TRACE_ID_HEADER, traceId))
                .build();
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();
        mutatedExchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);

        MDC.put(MDC_KEY, traceId);
        try {
            return chain.filter(mutatedExchange)
                    .doFinally(signal -> MDC.remove(MDC_KEY));
        } finally {
            // 响应式链装配完成后同步段即可清理；链路真正结束时 doFinally 兜底
            MDC.remove(MDC_KEY);
        }
    }

    @Override
    public int getOrder() {
        // 早于鉴权过滤器
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
