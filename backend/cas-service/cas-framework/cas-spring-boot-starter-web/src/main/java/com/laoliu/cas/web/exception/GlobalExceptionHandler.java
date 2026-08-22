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
        log.error("全局异常处理: ", e);
        log.error("请求URL: {}", request.getRequestURI());
        return CommonResult.internalServerError("服务器内部错误");
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<?> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("运行时异常: ", e);
        log.error("请求URL: {}", request.getRequestURI());
        return CommonResult.internalServerError("服务器内部错误");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        StringBuilder message = new StringBuilder();
        for (ObjectError error : allErrors) {
            message.append(error.getDefaultMessage()).append(";");
        }
        return CommonResult.badRequest(message.toString());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public CommonResult<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return CommonResult.methodNotAllowed(e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<?> handleBindException(BindException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        StringBuilder message = new StringBuilder();
        for (ObjectError error : allErrors) {
            message.append(error.getDefaultMessage()).append(";");
        }
        return CommonResult.badRequest(message.toString());
    }

    @ExceptionHandler(BusinessException.class)
    public CommonResult<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: {}", e.getMessage());
        Integer code = e.getCode();
        if (code != null) {
            return CommonResult.error(code, e.getMessage());
        }
        return CommonResult.internalServerError(e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public CommonResult<?> handleUnauthorizedException(UnauthorizedException e) {
        log.warn("未授权访问: {}", e.getMessage());
        return CommonResult.unauthorized(e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CommonResult<?> handleForbiddenException(ForbiddenException e) {
        log.warn("禁止访问: {}", e.getMessage());
        return CommonResult.forbidden(e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CommonResult<?> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn("资源未找到: {}", e.getMessage());
        return CommonResult.notFound(e.getMessage());
    }
}
