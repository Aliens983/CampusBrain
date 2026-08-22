package com.laoliu.cas.common.exception.code;

import com.laoliu.cas.common.exception.ErrorCode;

/**
 * 预约/审核相关错误码 (40001-40099)
 * <p>
 * 合并了原 ServiceStatusErrorCode 的错误码。
 *
 * @author forever-king
 */
public interface BookErrorCode {

    // ========== 预约 ==========

    ErrorCode BOOKING_NOT_FOUND = new ErrorCode(40001, "预约记录不存在");

    ErrorCode BOOKING_CANCEL_FAILED = new ErrorCode(40002, "取消失败，预约不存在或已处理");

    ErrorCode BOOKING_FAILED = new ErrorCode(40003, "预约失败");

    ErrorCode BOOKING_REPEATED = new ErrorCode(40009, "您已预约该服务，请勿重复提交");

    ErrorCode BOOKING_CAPACITY_FULL = new ErrorCode(40010, "该服务预约名额已满，请选择其他服务");

    // ========== 审核 ==========

    ErrorCode STATUS_NOT_FOUND = new ErrorCode(40004, "预约状态不存在");

    ErrorCode AUDIT_FAILED = new ErrorCode(40005, "审核操作失败");

    ErrorCode AUDIT_REASON_REQUIRED = new ErrorCode(40006, "拒绝时必须填写原因");

    ErrorCode INVALID_AUDIT_STATUS = new ErrorCode(40007, "无效的审核状态");

    ErrorCode USER_EMAIL_NOT_FOUND = new ErrorCode(40008, "用户邮箱未找到");

}
