package com.laoliu.auth;

/**
 * 认证与内网调用相关的请求头常量
 *
 * <p>网关验签 JWT 后，将用户身份通过以下请求头透传给下游服务；
 * 下游服务通过 {@link InternalSigner} 校验 {@link #HEADER_INTERNAL_SIGN}，
 * 防止绕过网关直连服务伪造身份
 *
 * @author forever-king
 */
public final class AuthConstants {

    private AuthConstants() {
    }

    /** 当前登录用户 ID 透传头 */
    public static final String HEADER_USER_ID = "X-User-Id";

    /** 当前登录用户角色透传头 */
    public static final String HEADER_USER_ROLE = "X-User-Role";

    /** 内网调用签名头 */
    public static final String HEADER_INTERNAL_SIGN = "X-Internal-Sign";

    /** 签名时间戳头（参与签名，防重放） */
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
}
