package com.laoliu.cas.common.exception.code;

import com.laoliu.cas.common.exception.ErrorCode;

/**
 * 通用错误码 (10001-10099)
 *
 * @author forever-king
 */
public interface CommonErrorCode {

    ErrorCode SUCCESS = new ErrorCode(0, "操作成功");

    ErrorCode INTERNAL_ERROR = new ErrorCode(10001, "服务器内部错误");

    ErrorCode BAD_REQUEST = new ErrorCode(10002, "请求参数错误");

    ErrorCode UNAUTHORIZED = new ErrorCode(10003, "未授权，请先登录");

    ErrorCode FORBIDDEN = new ErrorCode(10004, "权限不足");

    ErrorCode NOT_FOUND = new ErrorCode(10005, "资源不存在");

    ErrorCode TOKEN_EXPIRED = new ErrorCode(10006, "登录已过期，请重新登录");

    ErrorCode TOKEN_INVALID = new ErrorCode(10007, "Token 无效或格式错误");

    // ========== 基础设施 ==========

    ErrorCode EMAIL_SEND_FAILED = new ErrorCode(10010, "邮件发送失败");

    ErrorCode SMS_SEND_FAILED = new ErrorCode(10011, "短信发送失败");

    ErrorCode QR_CODE_FAILED = new ErrorCode(10012, "二维码生成失败");

    ErrorCode WEATHER_QUERY_FAILED = new ErrorCode(10013, "天气查询失败");

    ErrorCode FILE_UPLOAD_FAILED = new ErrorCode(10014, "文件上传失败");

    ErrorCode FILE_EMPTY = new ErrorCode(10015, "文件不能为空");

}
