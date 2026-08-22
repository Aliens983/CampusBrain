package com.laoliu.cas.common.exception;


/**
 * @author forever-king
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(Integer code, String message) {
        super(code, message);
    }

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(404, message);
        initCause(cause);
    }
}
