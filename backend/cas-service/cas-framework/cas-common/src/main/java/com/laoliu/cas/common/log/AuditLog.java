package com.laoliu.cas.common.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 业务审计日志统一格式.
 * <p>
 * 只记录“谁对什么做了什么、结果如何”，字段固定为 key=value，便于日志平台检索；
 * 调用方不得传入密码、令牌、验证码、请求体等敏感内容.
 *
 * @author forever-king
 */
public final class AuditLog {

    /** 独立 logger，便于在日志平台按 logger 名称过滤审计事件 */
    public static final String LOGGER_NAME = "CAS_AUDIT";

    private static final Logger AUDIT = LoggerFactory.getLogger(LOGGER_NAME);
    private static final int MAX_DETAIL_LENGTH = 300;

    private AuditLog() {
    }

    public static void success(String action, Long operatorId, String targetType, Object targetId, String detail) {
        AUDIT.info("audit action={} operatorId={} targetType={} targetId={} result=SUCCESS detail={}",
                action, value(operatorId), value(targetType), value(targetId), clean(detail));
    }

    public static void failure(String action, Long operatorId, String targetType, Object targetId, String detail) {
        AUDIT.warn("audit action={} operatorId={} targetType={} targetId={} result=FAIL detail={}",
                action, value(operatorId), value(targetType), value(targetId), clean(detail));
    }

    private static String value(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private static String clean(String detail) {
        if (detail == null || detail.isBlank()) {
            return "-";
        }
        String normalized = detail.replaceAll("\\s+", " ").trim();
        return normalized.length() <= MAX_DETAIL_LENGTH
                ? normalized
                : normalized.substring(0, MAX_DETAIL_LENGTH) + "...";
    }
}
