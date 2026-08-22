package com.laoliu.cas.web.config;

import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.result.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @ExceptionHandler(BusinessException.class)
    public CommonResult<?> handleBusinessException(BusinessException e) {
        log.error("Business exception: {}", e.getMessage());
        return CommonResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public CommonResult<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Illegal argument exception: {}", e.getMessage());
        return CommonResult.badRequest(e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public CommonResult<?> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return CommonResult.notFound("资源不存在: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public CommonResult<?> handleException(Exception e) {
        log.error("System exception: ", e);
        return CommonResult.internalServerError("系统内部错误");
    }
}
