package com.kb.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kb.infrastructure.common.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified API response wrapper.
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * 业务状态码，统一为数值（与 CAS CommonResult 对齐）：
     * 200 = 成功，其余见 {@link ErrorCode}。
     * 曾为 Object（int/String 混用），导致前端无法做类型安全的分支判断。
     */
    private Integer code;

    /** 响应消息 */
    private String message;

    /** 响应数据，泛型类型 */
    private T data;

    /** 响应时间戳（毫秒） */
    private Long timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(ErrorCode.SUCCESS.getCode())
                .message(ErrorCode.SUCCESS.getMessage())
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String detail) {
        return ApiResponse.<T>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage() + (detail != null ? ": " + detail : ""))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> ApiResponse<T> badRequest(String detail) {
        return error(ErrorCode.BAD_REQUEST, detail);
    }

    public static <T> ApiResponse<T> notFound(String detail) {
        return error(ErrorCode.NOT_FOUND, detail);
    }

    public static <T> ApiResponse<T> serverError(String detail) {
        return error(ErrorCode.INTERNAL_ERROR, detail);
    }
}
