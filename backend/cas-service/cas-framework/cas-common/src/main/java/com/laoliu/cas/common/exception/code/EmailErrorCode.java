package com.laoliu.cas.common.exception.code;

import com.laoliu.cas.common.exception.ErrorCode;

/**
 * 邮件相关错误码 —— 已废弃，请使用 {@link UserErrorCode} 或 {@link CommonErrorCode}。
 *
 * @author forever-king
 * @deprecated 使用 {@link UserErrorCode} 替代
 */
@Deprecated
public interface EmailErrorCode {

    ErrorCode EMAIL_NOT_PROVIDED = new ErrorCode(20015, "邮箱不能为空");

    ErrorCode EMAIL_FORMAT_ERROR = new ErrorCode(20008, "邮箱格式错误");

    ErrorCode EMAIL_SEND_FAILED = new ErrorCode(10010, "邮件发送失败");

    ErrorCode EMAIL_SEND_TOO_FREQUENTLY = new ErrorCode(20016, "验证码发送过于频繁");

    ErrorCode VERIFICATION_CODE_EXPIRED = new ErrorCode(20013, "验证码已过期");

    ErrorCode VERIFICATION_CODE_ERROR = new ErrorCode(20012, "验证码错误");

    ErrorCode VERIFICATION_CODE_NOT_FOUND = new ErrorCode(20014, "验证码不存在");

}
