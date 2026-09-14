package com.laoliu.cas.web.config;

import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.web.filter.TraceIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author forever-king
 */
@Slf4j
@RestControllerAdvice
@AutoConfiguration
public class WebAutoConfiguration {

    @Value("${file.upload.path:./uploads/}")
    private String uploadPath;

    @Bean
    public WebMvcConfigurer resourceConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
                String path = uploadPath;
                if (!path.endsWith("/")) {
                    path += "/";
                }
                registry.addResourceHandler("/api/files/**", "/uploads/**")
                        .addResourceLocations("file:" + path);
            }
        };
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 全链路 traceId 过滤器（5.2.1）：最高优先级注册，
     * 读取网关注入的 X-Trace-Id 写入 MDC 并回写响应头。
     */
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration =
                new FilterRegistrationBean<>(new TraceIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }

    // 注：CORS 已统一收口到网关 globalcors（4.1.8），本服务不再注册 CorsFilter。
    // 预检 OPTIONS 在网关直接应答，业务流量只来自网关内网转发。

    // ========== 异常处理 ==========
    //
    // 本服务另有一个专职的 GlobalExceptionHandler（cas.web.exception 包），
    // 已覆盖：BusinessException(400) / UnauthorizedException(401) / ForbiddenException(403) /
    // ResourceNotFoundException(404) / MethodArgumentNotValidException(400) /
    // NoResourceFoundException(404) / RuntimeException(500) / Exception(500)。
    //
    // 因此本类只保留下面这一个它未覆盖的类型。同一异常若被两个 @RestControllerAdvice
    // 同时声明，最终生效哪个取决于注册顺序，行为不确定——这是必须避免的。

    /**
     * 非法参数 → 400（客户端错误）。
     * 若不在此声明，会落到 GlobalExceptionHandler 的 RuntimeException 分支而返回 500，语义偏重。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return CommonResult.badRequest(e.getMessage());
    }
}
