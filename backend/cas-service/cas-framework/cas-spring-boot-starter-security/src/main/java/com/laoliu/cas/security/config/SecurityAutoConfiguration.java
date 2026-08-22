package com.laoliu.cas.security.config;

import com.laoliu.auth.InternalSigner;
import com.laoliu.cas.common.security.JWTUtils;
import com.laoliu.cas.security.filter.InternalAuthFilter;
import com.laoliu.cas.security.filter.JWTFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
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
@ComponentScan(
        basePackages = {"com.laoliu.cas.security", "com.laoliu.auth"},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.laoliu.auth.JWTUtils.class)
)
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JWTFilter jwtFilter, InternalAuthFilter internalAuthFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/**",
                                "/captcha",
                                "/error",
                                "/uploads/**",
                                "/doc.html",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/swagger-ui-layer/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/webjars/**",
                                "/favicon.ico",
                                "/hello",
                                "/weather/**",
                                "/config-demo/**",
                                "/sentinel-demo/**"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(internalAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
