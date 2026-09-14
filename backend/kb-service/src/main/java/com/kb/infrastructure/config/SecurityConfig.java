package com.kb.infrastructure.config;

import com.kb.infrastructure.security.TokenAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @PostConstruct
    public void init() {
        SecurityContextHolder.setStrategyName(
                SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/health").permitAll()
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
