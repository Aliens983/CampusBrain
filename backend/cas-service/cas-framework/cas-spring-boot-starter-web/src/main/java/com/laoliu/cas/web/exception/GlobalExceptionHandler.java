package com.laoliu.cas.web.exception;

import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.ErrorCode;
import com.laoliu.cas.common.exception.ForbiddenException;
import com.laoliu.cas.common.exception.ResourceNotFoundException;
import com.laoliu.cas.common.exception.UnauthorizedException;
import com.laoliu.cas.common.result.CommonResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * @author forever-king
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CommonResult<?> handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        log.warn("静态资源未找到: {} -> {}", request.getRequestURI(), e.getMessage());
        return CommonResult.notFound("资源未找到");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<?> handleException(Exception e, HttpServletRequest request) {
        log.error("unhandled_error path={} method={} userId={}",
                request.getRequestURI(), request.getMethod(), userId(request), e);
        return CommonResult.internalServerError("服务器内部错误");
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<?> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("unhandled_runtime_error path={} method={} userId={}",
                request.getRequestURI(), request.getMethod(), userId(request), e);
        return CommonResult.internalServerError("服务器内部错误");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e,
                                                                 HttpServletRequest request) {
        log.warn("validation_failed path={} method={} userId={} detail={}",
                request.getRequestURI(), request.getMethod(), userId(request), e.getMessage());
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        StringBuilder message = new StringBuilder();
        for (ObjectError error : allErrors) {
            message.append(error.getDefaultMessage()).append(";");
        }
        return CommonResult.badRequest(message.toString());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public CommonResult<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e,
                                                                       HttpServletRequest request) {
        log.warn("method_not_allowed path={} method={} userId={} detail={}",
                request.getRequestURI(), request.getMethod(), userId(request), e.getMessage());
        return CommonResult.methodNotAllowed(e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<?> handleBindException(BindException e, HttpServletRequest request) {
        log.warn("validation_failed path={} method={} userId={} detail={}",
                request.getRequestURI(), request.getMethod(), userId(request), e.getMessage());
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        StringBuilder message = new StringBuilder();
        for (ObjectError error : allErrors) {
            message.append(error.getDefaultMessage()).append(";");
        }
        return CommonResult.badRequest(message.toString());
    }

    /**
     * 业务异常：HTTP 语义与响应体 code 对齐.
     * <p>
     * 此前返回 HTTP 200，导致网关/Nginx 按状态码统计时业务失败被算作成功，
     * 5xx 告警失真；同时前端需要同时判断 status 与 body.code 两套口径.
     * 现在统一为 400（客户端可预期错误），细分原因仍由 body.code 表达.
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        Integer code = e.getCode();
        log.warn("business_error path={} method={} userId={} code={} message={}",
                request.getRequestURI(), request.getMethod(), userId(request), code, e.getMessage());
        if (code != null) {
            return CommonResult.error(code, e.getMessage());
        }
        return CommonResult.internalServerError(e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public CommonResult<?> handleUnauthorizedException(UnauthorizedException e, HttpServletRequest request) {
        log.warn("unauthorized path={} method={} userId={} message={}",
                request.getRequestURI(), request.getMethod(), userId(request), e.getMessage());
        return CommonResult.unauthorized(e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CommonResult<?> handleForbiddenException(ForbiddenException e, HttpServletRequest request) {
        log.warn("forbidden path={} method={} userId={} message={}",
                request.getRequestURI(), request.getMethod(), userId(request), e.getMessage());
        return CommonResult.forbidden(e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CommonResult<?> handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        log.warn("resource_not_found path={} method={} userId={} message={}",
                request.getRequestURI(), request.getMethod(), userId(request), e.getMessage());
        return CommonResult.notFound(e.getMessage());
    }

    /** 网关会透传 X-User-Id；直连/白名单请求没有该头，用 "-" 占位. */
    private static String userId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        return userId == null || userId.isBlank() ? "-" : userId;
    }
}
