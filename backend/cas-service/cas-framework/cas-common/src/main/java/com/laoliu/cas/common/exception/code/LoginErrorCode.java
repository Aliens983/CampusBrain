package com.laoliu.cas.common.exception.code;

import com.laoliu.cas.common.exception.ErrorCode;

/**
 * 登录相关错误码 —— 已废弃，请使用 {@link UserErrorCode}。
 *
 * @author forever-king
 * @deprecated 使用 {@link UserErrorCode} 替代
 */
@Deprecated
public interface LoginErrorCode {

    ErrorCode USER_NOT_EXIST = new ErrorCode(20001, "用户不存在");

    ErrorCode PASSWORD_ERROR = new ErrorCode(20005, "密码错误");

    ErrorCode LOGIN_FAILED = new ErrorCode(20006, "登录失败");

    ErrorCode TOKEN_EXPIRED = new ErrorCode(10006, "Token已过期");

    ErrorCode TOKEN_INVALID = new ErrorCode(10007, "Token无效");

    ErrorCode VERIFICATION_CODE_ERROR = new ErrorCode(20012, "验证码错误");

    ErrorCode VERIFICATION_CODE_EXPIRED = new ErrorCode(20013, "验证码已过期");

    ErrorCode USER_NOT_EXIST_BY_EMAIL = new ErrorCode(20010, "该邮箱未注册");

    ErrorCode EMAIL_OR_PASSWORD_EMPTY = new ErrorCode(20004, "邮箱和密码不能为空");

    ErrorCode EMAIL_EMPTY = new ErrorCode(20007, "邮箱不能为空");

    ErrorCode VERIFICATION_CODE_EMPTY = new ErrorCode(20011, "验证码不能为空");

    ErrorCode PASSWORD_EMPTY = new ErrorCode(20009, "密码不能为空");
}
