package com.laoliu.cas.security.config;

import com.laoliu.auth.InternalSigner;
import com.laoliu.auth.JWTUtils;
import com.laoliu.cas.security.filter.InternalAuthFilter;
import com.laoliu.cas.security.filter.JWTFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * @author forever-king
 */
@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
// Q-02：JWTUtils 收敛为 common-auth 唯一实现，随 com.laoliu.auth 包扫描装配（此前排除它是为了使用 cas 自带副本）
@ComponentScan(basePackages = {"com.laoliu.cas.security", "com.laoliu.auth"})
public class SecurityAutoConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JWTFilter jwtFilter(JWTUtils jwtUtils) {
        return new JWTFilter(jwtUtils);
    }

    @Bean
    public InternalAuthFilter internalAuthFilter(InternalSigner internalSigner) {
        return new InternalAuthFilter(internalSigner);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JWTFilter jwtFilter,
                                                   InternalAuthFilter internalAuthFilter,
                                                   @Value("${cas.security.swagger-enabled:false}") boolean swaggerEnabled)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                                    "/auth/**",
                                    // 同网关白名单：放行生成与取图两个子路径（P1-04）
                                    "/captcha/**",
                                    "/error",
                                    "/uploads/**",
                                    "/favicon.ico",
                                    "/weather/**",
                                    // 只读监控端点：仅由独立管理端口（默认 9101，容器内网、
                                    // 不发布宿主机）上的 Actuator 实际提供；业务 18080 上
                                    // 不存在这些映射，放行后访问业务端口只会得到 404
                                    "/actuator/health",
                                    "/actuator/health/**",
                                    "/actuator/info",
                                    "/actuator/prometheus"
                            ).permitAll();
                    // 4.14：接口文档默认不暴露，仅显式 cas.security.swagger-enabled=true（本地/内网开发）
                    // 时放行，生产环境 /doc.html 与 /v3/api-docs 均需认证，避免接口结构裸奔
                    if (swaggerEnabled) {
                        auth.requestMatchers(
                                "/doc.html",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/swagger-ui-layer/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        ).permitAll();
                    }
                    auth
                            // 授权唯一来源是方法级 @RequireRole（RoleAspect 实时查库判定角色），
                            // 路径前缀（/admin、/teacher、/app）不再承载权限语义；
                            // AdminEndpointAuthorizationGuardTest 守护：所有 /admin/** 映射必须标注 @RequireRole。
                            //
                            // 3.4.1/4.9：/appointments/** 仅供内网（经网关签名、ROLE_INTERNAL）调用：
                            // assistant 会话接口与可用性/我的预约查询均不面向终端 JWT 用户——前端零直连，
                            // 唯一调用方是 kb-service 的 Feign（CasClient，内网签名）。
                            // 此前仅 /appointments/assistant/** 受限，AvailabilityController#/mine
                            // 信任无签名请求里的 X-User-Id 兜底，绕过网关直连 18080 即可冒查任意人预约。
                            .requestMatchers("/appointments/**").hasRole("INTERNAL")
                            .anyRequest().authenticated();
                })
                .addFilterBefore(internalAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
