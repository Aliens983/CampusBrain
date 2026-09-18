package com.laoliu.auth.web;

/**
 * 过滤器层未认证错误体的统一序列化工具（Q-07）。
 * <p>
 * 此前网关 {@code AuthGlobalFilter}、KB {@code TokenAuthenticationFilter}、
 * CAS {@code InternalAuthFilter} 各自手写 401 JSON 字符串，字段口径与
 * 业务侧统一响应体（kb {@code ApiResponse} / cas {@code CommonResult}）漂移，
 * 且直接字符串拼接未转义。过滤器早于 Spring MVC 异常体系执行，
 * 无法复用 {@code @RestControllerAdvice}，故在此提供不依赖 Jackson 的
 * 公共序列化（消息体均为固定文案，仍做标准 JSON 转义）。
 * <p>
 * 统一字段：{@code code}（数值）、{@code message}、{@code timestamp}（毫秒），
 * 与 kb {@code ApiResponse}（NON_NULL 序列化为 code/message/timestamp）一致。
 *
 * @author forever-king
 */
public final class AuthErrorResponses {

    public static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";

    private AuthErrorResponses() {
    }

    /** 构造统一错误体 JSON（code/message/timestamp） */
    public static String body(int code, String message) {
        return "{\"code\":" + code
                + ",\"message\":\"" + escape(message) + "\""
                + ",\"timestamp\":" + System.currentTimeMillis()
                + "}";
    }

    /** 401 未认证错误体的便捷方法 */
    public static String unauthorized(String message) {
        return body(401, message);
    }

    /** 最小 JSON 字符串转义（RFC 8259 必须转义的字符 + 控制字符） */
    private static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
