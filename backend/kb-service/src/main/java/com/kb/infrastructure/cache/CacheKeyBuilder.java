package com.kb.infrastructure.cache;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 缓存 Key 统一构建器
 *
 * @author forever-king
 */
@Component
public class CacheKeyBuilder {

    private static final String PREFIX_QA = "qa:cache:";
    private static final String PREFIX_SESSION = "session:";
    private static final String PREFIX_HOT = "qa:hot:";
    private static final String PREFIX_RATE = "rate:";

    /**
     * 问答缓存 Key
     */
    public String qaCacheKey(String query) {
        return PREFIX_QA + sha256(query);
    }

    /**
     * 会话消息 Key
     */
    public String sessionKey(String sessionId) {
        return PREFIX_SESSION + sessionId + ":messages";
    }

    /**
     * 热点问题排行榜 Key
     */
    public String hotQueriesKey() {
        return PREFIX_HOT + "queries";
    }

    /**
     * 限流计数 Key
     */
    public String rateLimitKey(String userId, String action) {
        return PREFIX_RATE + userId + ":" + action;
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
