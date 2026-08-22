package com.laoliu.cas.appointment.application.service;

import com.laoliu.cas.appointment.interfaces.dto.request.ServiceStatusPageReqVO;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 服务预约状态应用服务接口
 *
 * @author forever-king
 */
public interface ServiceStatusService {

    /** 获取所有服务预约状态 */
    List<ServiceStatusResponse> getServiceStatus();

    /**
     * 分页查询所有服务预约状态（支持筛选）。
     */
    IPage<ServiceStatusResponse> getServiceStatus(ServiceStatusPageReqVO reqVO);

    /** 根据用户ID获取预约状态 */
    List<ServiceStatusResponse> getServiceStatusByUserId(Long userId);

    /**
     * 分页查询用户的预约状态（支持筛选）。
     */
    IPage<ServiceStatusResponse> getServiceStatusByUserId(Long userId, ServiceStatusPageReqVO reqVO);

    /** 获取用户预约状态（含描述） */
    List<ServiceStatusResponse> getServiceStatusByUserIdWithDescription(Long userId);

    /**
     * 分页查询用户的预约状态（含状态描述，支持筛选）。
     */
    IPage<ServiceStatusResponse> getServiceStatusByUserIdWithDescription(Long userId, ServiceStatusPageReqVO reqVO);

    /** 审核服务预约 */
    boolean auditService(Long orderId, Integer status, String reason);

    /** 根据订单ID获取预约状态 */
    ServiceStatusResponse getServiceStatusByOrderId(Long orderId);

    /** 发送审核邮件 */
    void sendAuditEmail(Long orderId, String title, String content);

    /**
     * 审核通过预约，发送通知邮件。
     *
     * @param orderId 订单ID
     * @param reason  审核备注（可选）
     */
    void auditPass(Long orderId, String reason);

    /**
     * 审核驳回预约，发送通知邮件。
     *
     * @param orderId 订单ID
     * @param reason  驳回原因（必填）
     */
    void auditReject(Long orderId, String reason);
}
