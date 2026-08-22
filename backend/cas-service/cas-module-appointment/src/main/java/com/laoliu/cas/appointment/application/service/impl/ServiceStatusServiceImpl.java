package com.laoliu.cas.appointment.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.ServiceStatusService;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.interfaces.dto.request.ServiceStatusPageReqVO;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;
import com.laoliu.cas.common.enums.ManageStatus;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.BookErrorCode;
import com.laoliu.cas.infra.application.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author forever-king
 */
@Service
@RequiredArgsConstructor
public class ServiceStatusServiceImpl implements ServiceStatusService {

    private final BookingRepository bookingRepository;
    private final EmailService emailService;

    @Override
    public List<ServiceStatusResponse> getServiceStatus() {
        return bookingRepository.getServiceStatus();
    }

    @Override
    public IPage<ServiceStatusResponse> getServiceStatus(ServiceStatusPageReqVO reqVO) {
        IPage<ServiceStatusResponse> result = bookingRepository.getServiceStatus(
                reqVO.getPageNo(), reqVO.getPageSize(),
                reqVO.getManageStatus(), reqVO.getServiceName());
        result.getRecords().forEach(this::setStatusDescription);
        return result;
    }

    @Override
    public List<ServiceStatusResponse> getServiceStatusByUserId(Long userId) {
        return bookingRepository.getServiceStatusByUserId(userId);
    }

    @Override
    public IPage<ServiceStatusResponse> getServiceStatusByUserId(Long userId, ServiceStatusPageReqVO reqVO) {
        return bookingRepository.getServiceStatusByUserId(userId, reqVO.getPageNo(), reqVO.getPageSize(),
                reqVO.getManageStatus(), reqVO.getServiceName());
    }

    @Override
    public List<ServiceStatusResponse> getServiceStatusByUserIdWithDescription(Long userId) {
        List<ServiceStatusResponse> statusList = bookingRepository.getServiceStatusByUserId(userId);
        statusList.forEach(this::setStatusDescription);
        return statusList;
    }

    @Override
    public IPage<ServiceStatusResponse> getServiceStatusByUserIdWithDescription(Long userId, ServiceStatusPageReqVO reqVO) {
        IPage<ServiceStatusResponse> statusPage = bookingRepository.getServiceStatusByUserId(
                userId, reqVO.getPageNo(), reqVO.getPageSize(),
                reqVO.getManageStatus(), reqVO.getServiceName());
        statusPage.getRecords().forEach(this::setStatusDescription);
        return statusPage;
    }

    @Override
    public boolean auditService(Long orderId, Integer status, String reason) {
        return bookingRepository.auditService(orderId, status, reason);
    }

    @Override
    public ServiceStatusResponse getServiceStatusByOrderId(Long orderId) {
        return bookingRepository.getServiceStatusByOrderId(orderId);
    }

    @Override
    public void sendAuditEmail(Long orderId, String title, String content) {
        String email = bookingRepository.getUserEmailByOrderId(orderId);
        if (email != null && !email.isEmpty()) {
            emailService.sendEmail(email, title, content);
        }
    }

    @Override
    public void auditPass(Long orderId, String reason) {
        ServiceStatusResponse serviceInfo = getServiceStatusByOrderId(orderId);
        if (serviceInfo == null) {
            throw new BusinessException(BookErrorCode.STATUS_NOT_FOUND);
        }

        boolean success = bookingRepository.auditService(orderId, ManageStatus.APPROVED.getCode(), reason);
        if (!success) {
            throw new BusinessException(BookErrorCode.AUDIT_FAILED);
        }

        String emailContent = "您好！您的预约已通过。\n预约服务：" + serviceInfo.getServiceName()
                + "\n服务描述：" + serviceInfo.getServiceDescribe()
                + (reason == null || reason.trim().isEmpty() ? "" : "\n备注：" + reason);
        sendAuditEmail(orderId, "预约审核通过通知", emailContent);
    }

    @Override
    public void auditReject(Long orderId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException(BookErrorCode.AUDIT_REASON_REQUIRED);
        }

        ServiceStatusResponse serviceInfo = getServiceStatusByOrderId(orderId);
        if (serviceInfo == null) {
            throw new BusinessException(BookErrorCode.STATUS_NOT_FOUND);
        }

        boolean success = bookingRepository.auditService(orderId, ManageStatus.REJECTED.getCode(), reason);
        if (!success) {
            throw new BusinessException(BookErrorCode.AUDIT_FAILED);
        }

        String emailContent = "您好！您的预约未通过。\n预约服务：" + serviceInfo.getServiceName()
                + "\n服务描述：" + serviceInfo.getServiceDescribe()
                + "\n拒绝原因：" + reason;
        sendAuditEmail(orderId, "预约审核未通过通知", emailContent);
    }

    private void setStatusDescription(ServiceStatusResponse response) {
        if (response.getManageStatus() != null) {
            switch (response.getManageStatus()) {
                case 0 -> response.setStatusDescription(ManageStatus.SUBMIT.getMessage());
                case 1 -> response.setStatusDescription(ManageStatus.APPROVED.getMessage());
                case 2 -> response.setStatusDescription(ManageStatus.REJECTED.getMessage());
                case 3 -> response.setStatusDescription(ManageStatus.CANCELLED.getMessage());
                default -> response.setStatusDescription("未知状态");
            }
        }
    }
}
