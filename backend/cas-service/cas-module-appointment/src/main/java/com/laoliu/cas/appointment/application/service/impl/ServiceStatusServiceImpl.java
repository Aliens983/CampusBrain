package com.laoliu.cas.appointment.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.AuditSource;
import com.laoliu.cas.appointment.application.service.ServiceStatusService;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.infrastructure.metrics.BookingMetrics;
import com.laoliu.cas.appointment.interfaces.dto.request.ServiceStatusPageRequest;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;
import com.laoliu.cas.common.enums.ManageStatus;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.BookErrorCode;
import com.laoliu.cas.infra.application.service.EmailService;
import com.laoliu.cas.system.application.service.NotificationSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author forever-king
 */
@Service
@RequiredArgsConstructor
public class ServiceStatusServiceImpl implements ServiceStatusService {

    private final BookingRepository bookingRepository;
    private final EmailService emailService;
    private final NotificationSettingsService notificationSettings;
    private final BookingMetrics bookingMetrics;

    @Override
    public IPage<ServiceStatusResponse> getServiceStatus(ServiceStatusPageRequest req) {
        IPage<ServiceStatusResponse> result = bookingRepository.getServiceStatus(
                req.getPageNo(), req.getPageSize(),
                req.getManageStatus(), req.getServiceName());
        result.getRecords().forEach(this::setStatusDescription);
        return result;
    }

    @Override
    public List<ServiceStatusResponse> getServiceStatusByUserId(Long userId) {
        return bookingRepository.getServiceStatusByUserId(userId);
    }

    @Override
    public IPage<ServiceStatusResponse> getServiceStatusByUserId(Long userId, ServiceStatusPageRequest req) {
        return bookingRepository.getServiceStatusByUserId(userId, req.getPageNo(), req.getPageSize(),
                req.getManageStatus(), req.getServiceName());
    }

    @Override
    public List<ServiceStatusResponse> getServiceStatusByUserIdWithDescription(Long userId) {
        List<ServiceStatusResponse> statusList = bookingRepository.getServiceStatusByUserId(userId);
        statusList.forEach(this::setStatusDescription);
        return statusList;
    }

    @Override
    public IPage<ServiceStatusResponse> getServiceStatusByUserIdWithDescription(Long userId, ServiceStatusPageRequest req) {
        IPage<ServiceStatusResponse> statusPage = bookingRepository.getServiceStatusByUserId(
                userId, req.getPageNo(), req.getPageSize(),
                req.getManageStatus(), req.getServiceName());
        statusPage.getRecords().forEach(this::setStatusDescription);
        return statusPage;
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

    /**
     * 审核通过：改状态 + （必要时）发通知。
     * <p>
     * 加事务是因为一次审核可能伴随多步写操作（状态、库存回补、时段释放），
     * 任一步失败都必须整体回滚，否则会留下"名额退了但时段仍被占"的不一致。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    // 审核会改变预约状态（进而影响可用余量判断），让 services 缓存立即失效
    @CacheEvict(value = "services", allEntries = true)
    public void auditPass(Long orderId, String reason, AuditSource source) {
        long startNanos = System.nanoTime();
        ServiceStatusResponse serviceInfo = getServiceStatusByOrderId(orderId);
        if (serviceInfo == null) {
            throw new BusinessException(BookErrorCode.STATUS_NOT_FOUND);
        }

        // 通过仅允许待审核单（状态机白名单）；已通过/已取消等单再点通过会失败
        boolean success = bookingRepository.auditService(
                orderId, ManageStatus.APPROVED.getCode(), reason, List.of(ManageStatus.SUBMIT));
        if (!success) {
            throw new BusinessException(BookErrorCode.AUDIT_FAILED);
        }
        bookingMetrics.recordAudit(true, source, System.nanoTime() - startNanos);

        // 3.1.8：邮件措辞区分审核人（咨询师本人 vs 管理员）
        String emailContent = "您好！您的预约已通过" + source.reviewerLabel() + "审核。\n预约服务："
                + serviceInfo.getServiceName()
                + "\n服务描述：" + serviceInfo.getServiceDescribe()
                + slotLine(serviceInfo)
                + (reason == null || reason.trim().isEmpty() ? "" : "\n备注：" + reason);
        // 通知策略门控：管理端邮件策略开启 且 用户邮件偏好开启 才发
        if (notificationSettings.isEmailAllowed(serviceInfo.getUserId())) {
            sendAuditEmail(orderId, "预约审核通过通知", emailContent);
        }
    }

    /**
     * 审核拒绝：改状态 + 释放库存 + 释放咨询时段 + 发通知，四步必须同成功同失败。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    // 拒绝会回补 booked_count 与咨询时段，同 auditPass 需要让余量快照失效
    @CacheEvict(value = "services", allEntries = true)
    public void auditReject(Long orderId, String reason, AuditSource source) {
        long startNanos = System.nanoTime();
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException(BookErrorCode.AUDIT_REASON_REQUIRED);
        }

        ServiceStatusResponse serviceInfo = getServiceStatusByOrderId(orderId);
        if (serviceInfo == null) {
            throw new BusinessException(BookErrorCode.STATUS_NOT_FOUND);
        }

        // 3.1.5：拒绝允许待审核单与已通过单（已通过单可被修正）。
        // 下方的释放库存/时段对两种来源都幂等安全：待审核单释放其占用，
        // 已通过单同样曾占用库存/时段，拒绝时一并回收。
        boolean success = bookingRepository.auditService(
                orderId, ManageStatus.REJECTED.getCode(), reason,
                List.of(ManageStatus.SUBMIT, ManageStatus.APPROVED));
        if (!success) {
            throw new BusinessException(BookErrorCode.AUDIT_FAILED);
        }
        bookingMetrics.recordAudit(false, source, System.nanoTime() - startNanos);

        // 审核拒绝：仅当该订单是通用类预约时回补库存（资源类预约下单从未扣减 booked_count，7.3.6）
        bookingRepository.releaseStockByOrderId(orderId);
        // 咨询时段预约：同时释放占用的老师时段
        bookingRepository.releaseSlotByOrderId(orderId);

        // 3.1.8：邮件措辞区分审核人（咨询师本人 vs 管理员）
        String emailContent = "您好！您的预约未通过" + source.reviewerLabel() + "审核。\n预约服务："
                + serviceInfo.getServiceName()
                + "\n服务描述：" + serviceInfo.getServiceDescribe()
                + slotLine(serviceInfo)
                + "\n拒绝原因：" + reason;
        // 通知策略门控：管理端邮件策略开启 且 用户邮件偏好开启 才发
        if (notificationSettings.isEmailAllowed(serviceInfo.getUserId())) {
            sendAuditEmail(orderId, "预约审核未通过通知", emailContent);
        }
    }

    /** 咨询/设备预约的邮件补充行（无资源明细返回空串） */
    private String slotLine(ServiceStatusResponse r) {
        StringBuilder sb = new StringBuilder();
        if (r.getConsultantName() != null) {
            sb.append("\n咨询师：").append(r.getConsultantName())
                    .append("\n咨询时段：").append(r.getSlotDate()).append(" ")
                    .append(r.getStartTime()).append("-").append(r.getEndTime());
        }
        if (r.getEquipmentName() != null) {
            sb.append("\n借用设备：").append(r.getEquipmentName())
                    .append(" × ").append(r.getQuantity() == null ? 1 : r.getQuantity())
                    .append("\n借用时段：").append(r.getSlotDate()).append(" ")
                    .append(r.getStartTime()).append("-").append(r.getEndTime());
        }
        return sb.toString();
    }

    /**
     * 状态中文描述统一经 {@link ManageStatus#of(Integer)} 取自枚举，
     * 与 {@code BookServiceImpl#getStatusDescription} 同源，消除重复的 switch 0..4。
     */
    private void setStatusDescription(ServiceStatusResponse response) {
        ManageStatus status = ManageStatus.of(response.getManageStatus());
        response.setStatusDescription(status == null ? "未知状态" : status.getMessage());
    }
}
