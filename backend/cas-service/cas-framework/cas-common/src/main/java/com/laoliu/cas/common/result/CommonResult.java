package com.laoliu.cas.common.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.laoliu.cas.common.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * @author forever-king
 */
@Data
public class CommonResult<T> implements Serializable {

    private Integer code;

    private String message;

    private T data;

    private CommonResult(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> CommonResult<T> success() {
        return new CommonResult<>(200, "操作成功", null);
    }

    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<>(200, "操作成功", data);
    }

    public static <T> CommonResult<T> success(String message, T data) {
        return new CommonResult<>(200, message, data);
    }

    public static <T> CommonResult<T> error(Integer code, String message) {
        return new CommonResult<>(code, message, null);
    }

    public static <T> CommonResult<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> CommonResult<T> error(ErrorCode errorCode, Object... params) {
        String message = String.format(errorCode.getMessage(), params);
        return error(errorCode.getCode(), message);
    }

    public static <T> CommonResult<T> error(CommonResult<?> commonResult) {
        return error(commonResult.getCode(), commonResult.getMessage());
    }

    public static <T> CommonResult<T> badRequest(String message) {
        return error(400, message);
    }

    public static <T> CommonResult<T> unauthorized(String message) {
        return error(401, message);
    }

    public static <T> CommonResult<T> forbidden(String message) {
        return error(403, message);
    }

    public static <T> CommonResult<T> notFound(String message) {
        return error(404, message);
    }

    public static <T> CommonResult<T> internalServerError(String message) {
        return error(500, message);
    }

    public static <T> CommonResult<T> methodNotAllowed(String message) {
        return error(405, message);
    }

    public boolean isSuccess() {
        return code != null && code == 200;
    }

    public boolean isError() {
        return !isSuccess();
    }

    @JsonIgnore
    public void checkError() {
        if (isSuccess()) {
            return;
        }
        throw new RuntimeException(message);
    }

    @JsonIgnore
    public T getCheckedData() {
        checkError();
        return data;
    }
}
