package com.laoliu.cas.appointment.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.AuditSource;
import com.laoliu.cas.appointment.application.service.ServiceStatusService;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.infrastructure.metrics.BookingMetrics;
import com.laoliu.cas.appointment.infrastructure.mq.BookingEventPublisher;
import com.laoliu.cas.appointment.application.dto.request.ServiceStatusPageRequest;
import com.laoliu.cas.appointment.domain.view.BookingQueryView;
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
    private final BookingEventPublisher bookingEventPublisher;

    @Override
    public IPage<BookingQueryView> getServiceStatus(ServiceStatusPageRequest req) {
        IPage<BookingQueryView> result = bookingRepository.getServiceStatus(
                req.getPageNo(), req.getPageSize(),
                req.getManageStatus(), req.getServiceName());
        result.getRecords().forEach(this::setStatusDescription);
        return result;
    }

    @Override
    public IPage<BookingQueryView> getServiceStatusByUserIdWithDescription(Long userId, ServiceStatusPageRequest req) {
        IPage<BookingQueryView> statusPage = bookingRepository.getServiceStatusByUserId(
                userId, req.getPageNo(), req.getPageSize(),
                req.getManageStatus(), req.getServiceName());
        statusPage.getRecords().forEach(this::setStatusDescription);
        return statusPage;
    }

    @Override
    public BookingQueryView getServiceStatusByOrderId(Long orderId) {
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
        BookingQueryView serviceInfo = getServiceStatusByOrderId(orderId);
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

        BookingQueryView serviceInfo = getServiceStatusByOrderId(orderId);
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


    /**
     * 管理员强制取消（3.4 僵尸单兜底）：用户侧只能取消待审核单或已通过的活动单，
     * 已通过的通用/咨询/教室/设备单若无人处理会永久占用名额/时段，
     * 由管理员端点无条件（限角色）回收。状态变更与资源释放在同一条多表 UPDATE 内原子完成。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    // 释放占用会改变余量，与 auditReject 同样需要让 services 余量快照立即失效
    @CacheEvict(value = "services", allEntries = true)
    public void adminForceCancel(Long orderId, String reason) {
        BookingQueryView serviceInfo = requireOrder(orderId);
        boolean success = bookingRepository.adminCancelAndRelease(orderId, normalizeReason(reason));
        if (!success) {
            // 0 行：订单已是取消/拒绝/完结等终态（状态机白名单），拒绝重复操作
            throw new BusinessException(BookErrorCode.AUDIT_FAILED);
        }

        // 3.4/3.5：管理员强制取消同样改变预约状态与余量，KB 问答缓存须收到事件
        // （发布器在事务 afterCommit 发送，回滚不发；serviceId 取订单真实归属）
        bookingEventPublisher.publishChanged(
                serviceInfo.getUserId(), serviceInfo.getServiceId(), "CANCELLED");

        String emailContent = "您好！您的以下预约已被管理员取消：\n预约服务："
                + serviceInfo.getServiceName()
                + slotLine(serviceInfo)
                + (reason == null || reason.trim().isEmpty() ? "" : "\n取消原因：" + reason);
        if (notificationSettings.isEmailAllowed(serviceInfo.getUserId())) {
            sendAuditEmail(orderId, "预约被管理员取消通知", emailContent);
        }
    }

    /**
     * 管理员强制完结（3.4 僵尸单兜底）：对 end_time 与 services.end_date 均为空、
     * 定时任务无法自动完结的长期有效通用单，由管理员人工置为已完成并回补名额，
     * 避免 booked_count 只增不减把服务容量永久锁死。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "services", allEntries = true)
    public void adminForceComplete(Long orderId, String reason) {
        BookingQueryView serviceInfo = requireOrder(orderId);
        boolean success = bookingRepository.adminCompleteAndRelease(orderId, normalizeReason(reason));
        if (!success) {
            throw new BusinessException(BookErrorCode.AUDIT_FAILED);
        }

        // 3.4/3.5：管理员完结同样发布事件（KB 消费端不区分类型，统一触发缓存失效）
        bookingEventPublisher.publishChanged(
                serviceInfo.getUserId(), serviceInfo.getServiceId(), "COMPLETED");

        String emailContent = "您好！您的以下预约已由管理员标记为已完成：\n预约服务："
                + serviceInfo.getServiceName()
                + slotLine(serviceInfo)
                + (reason == null || reason.trim().isEmpty() ? "" : "\n备注：" + reason);
        if (notificationSettings.isEmailAllowed(serviceInfo.getUserId())) {
            sendAuditEmail(orderId, "预约完结通知", emailContent);
        }
    }

    /** 订单必须存在，否则抛 404 语义错误码 */
    private BookingQueryView requireOrder(Long orderId) {
        BookingQueryView serviceInfo = getServiceStatusByOrderId(orderId);
        if (serviceInfo == null) {
            throw new BusinessException(BookErrorCode.STATUS_NOT_FOUND);
        }
        return serviceInfo;
    }

    /** 空白备注统一归一为 null，避免 SQL 动态片段把空串写进 reason 列 */
    private static String normalizeReason(String reason) {
        return reason == null || reason.trim().isEmpty() ? null : reason;
    }

    /** 咨询/设备预约的邮件补充行（无资源明细返回空串） */
    private String slotLine(BookingQueryView r) {
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
    private void setStatusDescription(BookingQueryView response) {
        ManageStatus status = ManageStatus.of(response.getManageStatus());
        response.setStatusDescription(status == null ? "未知状态" : status.getMessage());
    }
}
