package com.kb.interfaces.rest;

import com.kb.infrastructure.common.BusinessException;
import com.kb.infrastructure.common.ErrorCode;
import com.kb.interfaces.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Global exception handler — 按异常类型分发，统一返回 {@link ApiResponse}
 * <p>
 * 生产环境对 RuntimeException 等未知异常做消息脱敏，只返
 * 回通用提示
 * </p>
 * @author forever-king
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ========== 业务异常 ==========

    @ExceptionHandler(BusinessException.AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleAuth(BusinessException.AuthenticationException e) {
        log.warn("Auth error: code={}, detail={}", e.getErrorCode(), e.getDetail());
        return ApiResponse.error(e.getErrorCode());
    }

    @ExceptionHandler(BusinessException.UserException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleUser(BusinessException.UserException e) {
        log.warn("User error: code={}, detail={}", e.getErrorCode(), e.getDetail());
        return ApiResponse.error(e.getErrorCode());
    }

    @ExceptionHandler(BusinessException.DocumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleDocument(BusinessException.DocumentException e) {
        log.warn("Document error: code={}, detail={}", e.getErrorCode(), e.getDetail());
        return ApiResponse.error(e.getErrorCode());
    }

    @ExceptionHandler(BusinessException.QaException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleQa(BusinessException.QaException e) {
        log.error("QA error: code={}, detail={}", e.getErrorCode(), e.getDetail());
        return ApiResponse.error(e.getErrorCode());
    }

    @ExceptionHandler(BusinessException.RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiResponse<Void> handleRateLimit(BusinessException.RateLimitException e) {
        log.warn("Rate limit: code={}", e.getErrorCode());
        return ApiResponse.error(e.getErrorCode());
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleBusiness(BusinessException e) {
        log.error("Business error: code={}, detail={}", e.getErrorCode(), e.getDetail());
        return ApiResponse.error(e.getErrorCode());
    }

    // ========== 参数校验 ==========

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return ApiResponse.error(ErrorCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Bad request: {}", e.getMessage());
        return ApiResponse.error(ErrorCode.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleFileTooLarge(MaxUploadSizeExceededException e) {
        return ApiResponse.error(ErrorCode.DOCUMENT_TOO_LARGE);
    }

    // ========== Catch-all（消息脱敏） ==========

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleRuntime(RuntimeException e) {
        log.error("Internal server error", e);
        // 不返回 e.getMessage()，防止泄露 SQL/路径/密钥等内部信息
        return ApiResponse.error(ErrorCode.INTERNAL_ERROR);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGeneral(Exception e) {
        log.error("Unexpected error", e);
        return ApiResponse.error(ErrorCode.INTERNAL_ERROR);
    }
}
