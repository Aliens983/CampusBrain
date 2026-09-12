package com.laoliu.cas.common.exception.code;

import com.laoliu.cas.common.exception.ErrorCode;

/**
 * 预约/审核相关错误码 (40001-40099)
 * <p>
 * 合并了原 ServiceStatusErrorCode 的错误码
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

    // ========== 咨询时段预约 ==========

    ErrorCode CONSULTANT_NOT_FOUND = new ErrorCode(40011, "咨询师不存在或不可预约");

    ErrorCode SLOT_UNAVAILABLE = new ErrorCode(40012, "该时段不可用或刚被他人预约，请重新选择");

    ErrorCode SLOT_MISMATCH = new ErrorCode(40013, "时段与咨询师不匹配");

    // ========== 设备借用 ==========

    ErrorCode EQUIPMENT_NOT_FOUND = new ErrorCode(40014, "设备不存在或不可借用");

    ErrorCode EQUIPMENT_STOCK_NOT_ENOUGH = new ErrorCode(40015, "该时段设备库存不足，请减少数量或换时段");

    ErrorCode BORROW_TIME_INVALID = new ErrorCode(40016, "借用时间不合法，请检查日期与起止时段");

    /**
     * 设备借用必须走专用端点。
     * 通用下单只传 serviceId，拿不到 equipmentId / 借用时段 / 数量，
     * 既无法参与设备的时段占用校验（可超借），又会让 services.booked_count
     * 与 equipment.available_stock 两套库存口径分裂。
     */
    ErrorCode EQUIPMENT_REQUIRE_DEDICATED_API = new ErrorCode(40020, "设备借用请使用设备专用接口：POST /app/equipment/{equipmentId}/book");

    // ========== 教室时段预约 ==========

    ErrorCode BOOK_TIME_INVALID = new ErrorCode(40017, "预约时间段不合法，请检查日期与起止时间");

    ErrorCode ROOM_NOT_FOUND = new ErrorCode(40018, "教室不存在或不可预约");

    ErrorCode ROOM_OCCUPIED = new ErrorCode(40019, "该教室此时间段已被预约，请换教室或时段");

    // ========== 审核 ==========

    ErrorCode STATUS_NOT_FOUND = new ErrorCode(40004, "预约状态不存在");

    ErrorCode AUDIT_FAILED = new ErrorCode(40005, "审核操作失败");

    ErrorCode AUDIT_REASON_REQUIRED = new ErrorCode(40006, "拒绝时必须填写原因");

    ErrorCode INVALID_AUDIT_STATUS = new ErrorCode(40007, "无效的审核状态");

    ErrorCode USER_EMAIL_NOT_FOUND = new ErrorCode(40008, "用户邮箱未找到");

}
