package com.laoliu.cas.appointment.application.service;

import com.laoliu.cas.appointment.interfaces.dto.request.ServiceStatusPageRequest;
import com.laoliu.cas.appointment.domain.view.BookingQueryView;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 服务预约状态应用服务接口
 *
 * @author forever-king
 */
public interface ServiceStatusService {

    /**
     * 分页查询所有服务预约状态（支持筛选）
     */
    IPage<BookingQueryView> getServiceStatus(ServiceStatusPageRequest req);

    /** 根据用户ID获取预约状态 */
    List<BookingQueryView> getServiceStatusByUserId(Long userId);

    /**
     * 分页查询用户的预约状态（支持筛选）
     */
    IPage<BookingQueryView> getServiceStatusByUserId(Long userId, ServiceStatusPageRequest req);

    /** 获取用户预约状态（含描述） */
    List<BookingQueryView> getServiceStatusByUserIdWithDescription(Long userId);

    /**
     * 分页查询用户的预约状态（含状态描述，支持筛选）
     */
    IPage<BookingQueryView> getServiceStatusByUserIdWithDescription(Long userId, ServiceStatusPageRequest req);

    /** 根据订单ID获取预约状态 */
    BookingQueryView getServiceStatusByOrderId(Long orderId);

    /** 发送审核邮件 */
    void sendAuditEmail(Long orderId, String title, String content);

    /**
     * 审核通过预约，发送通知邮件
     *
     * @param orderId 订单ID
     * @param reason  审核备注（可选）
     * @param source  审核人身份（管理员 / 咨询师），用于区分邮件措辞，3.1.8
     */
    void auditPass(Long orderId, String reason, AuditSource source);

    /**
     * 审核驳回预约，发送通知邮件
     *
     * @param orderId 订单ID
     * @param reason  驳回原因（必填）
     * @param source  审核人身份（管理员 / 咨询师），用于区分邮件措辞，3.1.8
     */
    void auditReject(Long orderId, String reason, AuditSource source);
}
