package com.kb.infrastructure.client;

import com.laoliu.auth.AuthConstants;
import com.laoliu.auth.InternalSigner;
import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CAS Feign 客户端配置 — 注入内网签名头，标识 KB 为受信服务。
 * <p>
 * CAS 的 {@code InternalAuthFilter} 校验 X-Internal-Sign 的 HMAC 签名与时间戳新鲜度，
 * 以此区分"服务间调用"与"伪造身份"，并放行到业务接口。
 * </p>
 *
 * @author forever-king
 */
@Configuration
@RequiredArgsConstructor
public class CasFeignConfig {

    /** 服务身份的用户 ID（非真实用户，仅用于签名） */
    private static final String SERVICE_USER_ID = "0";

    /** 服务身份角色标记 */
    private static final String SERVICE_ROLE = "SERVICE";

    private final InternalSigner internalSigner;

    @Bean
    public RequestInterceptor casInternalAuthInterceptor() {
        return template -> {
            String userId = SERVICE_USER_ID;
            String role = SERVICE_ROLE;
            String timestamp = String.valueOf(System.currentTimeMillis());
            String sign = internalSigner.sign(userId, role, timestamp);

            template.header(AuthConstants.HEADER_USER_ID, userId);
            template.header(AuthConstants.HEADER_USER_ROLE, role);
            template.header(AuthConstants.HEADER_TIMESTAMP, timestamp);
            template.header(AuthConstants.HEADER_INTERNAL_SIGN, sign);
        };
    }
}
