package com.laoliu.cas.common.exception.code;

import com.laoliu.cas.common.exception.ErrorCode;

/**
 * 服务相关错误码 (30001-30099)
 *
 * @author forever-king
 */
public interface ServiceErrorCode {

    ErrorCode SERVICE_NOT_FOUND = new ErrorCode(30001, "服务不存在");

    ErrorCode SERVICE_ALREADY_BOOKED = new ErrorCode(30002, "您已预约过该服务");

    ErrorCode SERVICE_NOT_ENABLED = new ErrorCode(30003, "服务已下架");

    ErrorCode SERVICE_ID_EMPTY = new ErrorCode(30004, "服务ID列表不能为空");

    ErrorCode SERVICE_NOT_EXIST = new ErrorCode(30005, "服务不存在: {0}");

    ErrorCode SERVICE_DISABLED = new ErrorCode(30006, "服务已被禁用: {0}");

    // ========== 预约操作 ==========

    ErrorCode SERVICE_BOOK_FAILED = new ErrorCode(30007, "服务预约失败");

    ErrorCode SERVICE_CANCEL_FAILED = new ErrorCode(30008, "取消预约失败");

}
