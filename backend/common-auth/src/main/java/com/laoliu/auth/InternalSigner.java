package com.laoliu.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 内网调用签名工具
 *
 * <p>网关验签 JWT 后，对透传身份头计算 {@code X-Internal-Sign}；
 * 下游服务用同一密钥校验签名，防止绕过网关直连服务伪造身份
 *
 * @author forever-king
 */
@Slf4j
@Component
public class InternalSigner {

    @Value("${auth.internal-sign-secret:internal-sign-default-secret}")
    private String secret;

    /**
     * 对若干参与签名的片段生成 HMAC-SHA256 签名（Base64）
     */
    public String sign(String... parts) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String payload = String.join("|", parts);
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to sign internal request", e);
        }
    }

    /**
     * 常量时间比较，校验签名是否匹配
     */
    public boolean verify(String signature, String... parts) {
        if (signature == null) {
            return false;
        }
        String expected = sign(parts);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 校验签名并检查时间戳新鲜度，防止签名头被重放
     *
     * @param signature    待校验的签名
     * @param maxAgeSeconds 允许的最大时间偏差（秒）
     * @param parts        参与签名的片段，最后一个元素必须是时间戳（毫秒）
     */
    public boolean verifyWithTimestamp(String signature, long maxAgeSeconds, String... parts) {
        if (!verify(signature, parts)) {
            return false;
        }
        if (parts.length == 0) {
            return false;
        }
        String timestampStr = parts[parts.length - 1];
        try {
            long timestamp = Long.parseLong(timestampStr);
            long now = System.currentTimeMillis();
            return Math.abs(now - timestamp) <= maxAgeSeconds * 1000L;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
