package com.laoliu.cas.common.exception.code;

import com.laoliu.cas.common.exception.ErrorCode;

/**
 * 用户/认证/角色 错误码 (20001-20099)
 * <p>
 * 合并了原 LoginErrorCode、EmailErrorCode、RoleErrorCode 的错误码。
 *
 * @author forever-king
 */
public interface UserErrorCode {

    // ========== 用户 ==========

    ErrorCode USER_NOT_FOUND = new ErrorCode(20001, "用户不存在");

    /** @deprecated 使用 {@link #USER_NOT_FOUND} */
    ErrorCode USER_NOT_EXIST = USER_NOT_FOUND;

    ErrorCode USER_ALREADY_EXISTS = new ErrorCode(20002, "该邮箱已被注册");

    ErrorCode USER_INFO_ERROR = new ErrorCode(20003, "用户信息错误");

    /** 邮箱和验证码不能同时为空 */
    ErrorCode EMAIL_OR_CODE_EMPTY = new ErrorCode(20022, "邮箱和验证码不能为空");

    // ========== 登录/认证 ==========

    ErrorCode EMAIL_OR_PASSWORD_EMPTY = new ErrorCode(20004, "邮箱和密码不能为空");

    ErrorCode PASSWORD_ERROR = new ErrorCode(20005, "密码错误");

    ErrorCode LOGIN_FAILED = new ErrorCode(20006, "登录失败");

    ErrorCode EMAIL_EMPTY = new ErrorCode(20007, "邮箱不能为空");

    ErrorCode EMAIL_FORMAT_ERROR = new ErrorCode(20008, "邮箱格式错误");

    ErrorCode PASSWORD_EMPTY = new ErrorCode(20009, "密码不能为空");

    ErrorCode USER_NOT_EXIST_BY_EMAIL = new ErrorCode(20010, "该邮箱未注册");

    // ========== 验证码 ==========

    ErrorCode VERIFICATION_CODE_EMPTY = new ErrorCode(20011, "验证码不能为空");

    ErrorCode VERIFICATION_CODE_ERROR = new ErrorCode(20012, "验证码错误");

    ErrorCode VERIFICATION_CODE_EXPIRED = new ErrorCode(20013, "验证码不存在或已过期");

    ErrorCode VERIFICATION_CODE_NOT_FOUND = new ErrorCode(20014, "验证码不存在");

    // ========== 邮件 ==========

    ErrorCode EMAIL_NOT_PROVIDED = new ErrorCode(20015, "邮箱不能为空");

    ErrorCode EMAIL_SEND_TOO_FREQUENTLY = new ErrorCode(20016, "验证码发送过于频繁，请60秒后再试");

    // ========== 角色 ==========

    ErrorCode ROLE_NOT_FOUND = new ErrorCode(20017, "角色不存在");

    ErrorCode ROLE_UPDATE_FAILED = new ErrorCode(20018, "角色更新失败");

    ErrorCode PERMISSION_DENIED = new ErrorCode(20019, "权限不足");

    ErrorCode ADMIN_ROLE_REQUIRED = new ErrorCode(20020, "需要管理员权限");

    ErrorCode USER_ROLE_ERROR = new ErrorCode(20021, "用户角色权限不足");

}
