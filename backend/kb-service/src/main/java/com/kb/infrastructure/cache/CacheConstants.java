package com.kb.infrastructure.cache;

/**
 * 统一 Redis Key 前缀常量。
 * <p>
 * 所有缓存 key 集中管理，避免散落分散和命名冲突。
 * </p>
 * @author forever-king
 */
public final class CacheConstants {

    private CacheConstants() {}

    // ---- 认证 ----
    public static final String ACCESS_TOKEN = "token:access:";
    public static final String REFRESH_TOKEN = "token:refresh:";

    // ---- 邮箱验证 ----
    public static final String VERIFICATION_CODE = "verification:code:";
    public static final String EMAIL_RATE_LIMIT = "rate_limit:email:";

    // ---- 限流 ----
    public static final String RATE_LIMIT = "rate_limit:";

    // ---- 问答缓存 ----
    public static final String QA_EXACT_CACHE = "qa:exact:";
    public static final String QA_SEMANTIC_CACHE = "qa:semantic:";

    // ---- 会话 ----
    public static final String SESSION = "session:";
}
