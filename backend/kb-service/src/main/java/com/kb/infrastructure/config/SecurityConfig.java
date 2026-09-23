package com.kb.infrastructure.config;

import com.kb.infrastructure.security.TokenAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 主配置
 * <p>
 * 无状态 API 认证 + 双 Token 过滤器 + RBAC 权限控制
 * </p>
 * <p>
 * CORS 统一在网关 globalcors 处理（4.1.8），本服务不配置 CORS；
 * 预检 OPTIONS 由网关直接应答，不会到达此处。
 * </p>
 *
 * @author forever-king
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenAuthenticationFilter tokenFilter;

    // 4.2（深度审查 P0）：不再使用 MODE_INHERITABLETHREADLOCAL。
    // 继承模式会让线程池里复用的线程（boundedElastic/OkHttp 回调）继承创建它的
    // 那一次请求的身份并永久残留 —— 后续请求的 Feign 调用会以他人身份执行（串号）。
    // 恢复默认 MODE_THREADLOCAL：池化线程拿不到身份时，Feign 回退 ChatContextHolder
    // 请求级上下文，再取不到则用服务身份，杜绝跨用户串号。

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/health").permitAll()
                        // 只读监控端点：仅由独立管理端口（默认 9102，容器内网、不发布宿主机）
                        // 上的 Actuator 实际提供；业务 8081 上不存在这些映射（放行后为 404），
                        // 网关亦未放行 /actuator，外部不可达
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/prometheus"
                        ).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        // 文档上传仅限管理员；删除允许「owner 本人或管理员」，
                        // 具体归属判定由 DocumentApplicationService 完成（4.1.12）
                        .requestMatchers(HttpMethod.POST, "/documents/upload")
                            .hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .securityContext(sc -> sc.requireExplicitSave(false))
                .addFilterBefore(tokenFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
