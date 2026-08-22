package com.laoliu.cas.common.exception;

import lombok.Data;

/**
 * @author forever-king
 */
@Data
public class ErrorCode {

    private final Integer code;
    private final String message;

    public ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}
