package com.laoliu.cas.common.exception.code;

import com.laoliu.cas.common.exception.ErrorCode;

/**
 * 预约状态相关错误码 —— 已废弃，请使用 {@link BookErrorCode}。
 *
 * @author forever-king
 * @deprecated 使用 {@link BookErrorCode} 替代
 */
@Deprecated
public interface ServiceStatusErrorCode {

    ErrorCode STATUS_NOT_FOUND = new ErrorCode(40004, "预约状态不存在");

    ErrorCode AUDIT_FAILED = new ErrorCode(40005, "审核操作失败");

    ErrorCode AUDIT_REASON_REQUIRED = new ErrorCode(40006, "拒绝时必须填写原因");

    ErrorCode INVALID_AUDIT_STATUS = new ErrorCode(40007, "无效的审核状态");

    ErrorCode USER_EMAIL_NOT_FOUND = new ErrorCode(40008, "用户邮箱未找到");

}
