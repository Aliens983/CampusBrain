package com.laoliu.auth.exception;

import lombok.Getter;

/**
 * 认证相关异常（common-auth 自包含）
 *
 * @author forever-king
 */
@Getter
public class AuthException extends RuntimeException {

    private final int code;

    public AuthException(int code, String message) {
        super(message);
        this.code = code;
    }

}
