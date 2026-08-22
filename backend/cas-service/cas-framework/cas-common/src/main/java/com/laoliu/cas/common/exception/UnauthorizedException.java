package com.laoliu.cas.common.exception;


/**
 * @author forever-king
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(Integer code, String message) {
        super(code, message);
    }

    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(401, message);
        initCause(cause);
    }
}
