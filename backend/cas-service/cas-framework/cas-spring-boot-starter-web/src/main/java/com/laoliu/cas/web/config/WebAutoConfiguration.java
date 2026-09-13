package com.laoliu.cas.web.config;

import com.laoliu.cas.common.result.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
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
