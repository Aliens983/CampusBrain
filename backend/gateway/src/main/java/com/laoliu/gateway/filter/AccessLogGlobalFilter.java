package com.laoliu.gateway.filter;

import com.laoliu.auth.AuthConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.net.InetSocketAddress;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * 网关访问日志：每个请求一行，覆盖鉴权通过后的全部路由与下游错误.
 * <p>
 * 放在 AuthGlobalFilter 之后，因此能拿到网关注入的 userId/role；
 * 鉴权失败的 401 由 AuthGlobalFilter 自己记录，避免访问日志漏掉失败请求.
 * 不记录 query/body/token，防止令牌与业务数据进入日志.
 *
 * @author forever-king
 */
@Component
public class AccessLogGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("ACCESS_LOG");
    private static final String UNKNOWN = "-";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startMs = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod() == null ? UNKNOWN : request.getMethod().name();
        String path = request.getURI().getPath();
        String clientIp = clientIp(request);

        return chain.filter(exchange).doFinally(signal -> {
            ServerHttpResponse response = exchange.getResponse();
            HttpStatusCode statusCode = response.getStatusCode();
            String status = statusCode != null
                    ? String.valueOf(statusCode.value())
                    : switch (signal) {
                        case ON_ERROR -> "500";
                        case CANCEL -> "499";
                        default -> "200";
                    };
            Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
            String routeId = route == null ? UNKNOWN : route.getId();
            String userId = headerOrUnknown(request, AuthConstants.HEADER_USER_ID);
            String role = headerOrUnknown(request, AuthConstants.HEADER_USER_ROLE);
            String traceId = headerOrUnknown(request, TraceIdGlobalFilter.TRACE_ID_HEADER);
            long costMs = System.currentTimeMillis() - startMs;

            String line = "method=" + method
                    + " path=" + path
                    + " status=" + status
                    + " costMs=" + costMs
                    + " route=" + routeId
                    + " userId=" + userId
                    + " role=" + role
                    + " ip=" + clientIp
                    + " traceId=" + traceId;

            boolean error = statusCode != null
                    ? statusCode.is4xxClientError() || statusCode.is5xxServerError()
                    : signal == SignalType.ON_ERROR;
            if (error) {
                ACCESS_LOG.warn("access {}", line);
            } else {
                ACCESS_LOG.info("access {}", line);
            }
        });
    }

    private static String headerOrUnknown(ServerHttpRequest request, String name) {
        String value = request.getHeaders().getFirst(name);
        return value == null || value.isBlank() ? UNKNOWN : value;
    }

    private static String clientIp(ServerHttpRequest request) {
        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        InetSocketAddress remote = request.getRemoteAddress();
        return remote != null && remote.getAddress() != null
                ? remote.getAddress().getHostAddress()
                : UNKNOWN;
    }

    @Override
    public int getOrder() {
        // 晚于 TraceIdGlobalFilter(HIGHEST) 与 AuthGlobalFilter(-100)，先拿到身份头再包住下游链
        return -90;
    }
}
