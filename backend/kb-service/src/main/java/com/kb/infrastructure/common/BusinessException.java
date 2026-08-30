package com.kb.infrastructure.common;

import lombok.Getter;

/**
 * 统一业务异常基类
 * <p>
 * 所有业务异常都应继承此类，携带 {@link ErrorCode} 和可选详情，
 * 由 {@code GlobalExceptionHandler} 统一处理并返回标准化响应
 * </p>
 * @author forever-king
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detail;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + (detail != null ? ": " + detail : ""));
        this.errorCode = errorCode;
        this.detail = detail;
    }

    // ===== 认证子类 =====

    public static class AuthenticationException extends BusinessException {
        public AuthenticationException(ErrorCode code) { super(code); }
        public AuthenticationException(ErrorCode code, String detail) { super(code, detail); }
    }

    // ===== 用户子类 =====

    public static class UserException extends BusinessException {
        public UserException(ErrorCode code) { super(code); }
        public UserException(ErrorCode code, String detail) { super(code, detail); }
    }

    // ===== 文档子类 =====

    public static class DocumentException extends BusinessException {
        public DocumentException(ErrorCode code) { super(code); }
        public DocumentException(ErrorCode code, String detail) { super(code, detail); }
    }

    // ===== 问答子类 =====

    public static class QaException extends BusinessException {
        public QaException(ErrorCode code) { super(code); }
        public QaException(ErrorCode code, String detail) { super(code, detail); }
    }

    // ===== 限流子类 =====

    public static class RateLimitException extends BusinessException {
        public RateLimitException(ErrorCode code) { super(code); }
        public RateLimitException(ErrorCode code, String detail) { super(code, detail); }
    }
}
