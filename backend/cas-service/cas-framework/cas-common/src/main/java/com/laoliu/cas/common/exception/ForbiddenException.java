package com.laoliu.cas.common.exception;


/**
 * @author forever-king
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(Integer code, String message) {
        super(code, message);
    }

    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(403, message);
        initCause(cause);
    }
}
