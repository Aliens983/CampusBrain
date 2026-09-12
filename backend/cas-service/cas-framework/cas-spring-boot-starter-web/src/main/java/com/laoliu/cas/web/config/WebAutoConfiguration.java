package com.laoliu.cas.web.config;

import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.ForbiddenException;
import com.laoliu.cas.common.exception.ResourceNotFoundException;
import com.laoliu.cas.common.exception.UnauthorizedException;
import com.laoliu.cas.common.result.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * @author forever-king
 */
@Slf4j
@RestControllerAdvice
@AutoConfiguration
public class WebAutoConfiguration {

    @Value("${file.upload.path:./uploads/}")
    private String uploadPath;

    /**
     * 允许跨域的站点，逗号分隔。
     * 默认仅放行本地开发端口；生产/演示需要放开其它域名时（如 cpolar 内网穿透），
     * 通过环境变量或 Nacos 配置 cas.cors.allowed-origins 覆盖，例如：
     * cas.cors.allowed-origins=http://localhost:3000,https://xxx.cpolar.cn
     * <p>
     * 注意：本服务与 allowCredentials=true 配合，切勿再使用 "*" 全放行，
     * 否则任意站点都可携带凭证访问本服务接口。
     */
    @Value("${cas.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:80,http://localhost}")
    private String[] allowedOrigins;

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

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        for (String origin : allowedOrigins) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty()) {
                config.addAllowedOriginPattern(trimmed);
            }
        }
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    // ========== 业务异常（HTTP 语义与 body.code 保持一致） ==========

    /**
     * 业务异常：此前没有 @ResponseStatus，导致业务失败也返回 HTTP 200，
     * 与网关/前端的 HTTP 语义判断不一致。现统一为 400。
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<?> handleBusinessException(BusinessException e) {
        // 业务异常是可预期分支，不是系统故障，用 warn 即可
        log.warn("Business exception: code={}, message={}", e.getCode(), e.getMessage());
        return CommonResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public CommonResult<?> handleUnauthorized(UnauthorizedException e) {
        log.warn("Unauthorized: {}", e.getMessage());
        return CommonResult.unauthorized(e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CommonResult<?> handleForbidden(ForbiddenException e) {
        log.warn("Forbidden: {}", e.getMessage());
        return CommonResult.forbidden(e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CommonResult<?> handleResourceNotFound(ResourceNotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return CommonResult.notFound(e.getMessage());
    }

    // ========== 参数校验 ==========

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("Validation failed: {}", msg);
        return CommonResult.badRequest(msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return CommonResult.badRequest(e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CommonResult<?> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return CommonResult.notFound("资源不存在: " + e.getMessage());
    }

    // ========== Catch-all（消息脱敏） ==========

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<?> handleException(Exception e) {
        // 完整堆栈只进日志，绝不回传前端，避免泄露 SQL / 路径 / 配置等内部信息
        log.error("System exception: ", e);
        return CommonResult.internalServerError("系统内部错误");
    }
}
