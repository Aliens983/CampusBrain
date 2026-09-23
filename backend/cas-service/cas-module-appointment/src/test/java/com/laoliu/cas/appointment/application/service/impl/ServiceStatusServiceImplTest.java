package com.laoliu.cas.appointment.application.service.impl;

import com.laoliu.cas.appointment.infrastructure.metrics.BookingMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import com.laoliu.cas.appointment.application.service.AuditSource;
import com.laoliu.cas.appointment.application.service.ServiceStatusService;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.infrastructure.mq.BookingEventPublisher;
import com.laoliu.cas.appointment.domain.view.BookingQueryView;
import com.laoliu.cas.common.enums.ManageStatus;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.BookErrorCode;
import com.laoliu.cas.infra.api.email.EmailService;
import com.laoliu.cas.system.api.NotificationSettingsApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ServiceStatusServiceImpl 单元测试
 * 使用 Given-When-Then 模式，覆盖审核通过/驳回的所有分支
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("预约审核服务单元测试")
class ServiceStatusServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationSettingsApi notificationSettings;

    @Mock
    private BookingEventPublisher bookingEventPublisher;

    private ServiceStatusService serviceStatusService;

    private static final Long VALID_ORDER_ID = 1L;
    private static final Long INVALID_ORDER_ID = 999L;
    private static final Long BOOKING_USER_ID = 100L;
    private static final Long BOOKING_SERVICE_ID = 200L;

    @BeforeEach
    void setUp() {
        serviceStatusService = new ServiceStatusServiceImpl(bookingRepository, emailService, notificationSettings,
                new BookingMetrics(new SimpleMeterRegistry()), bookingEventPublisher);
        // 默认"允许发送邮件"（策略与偏好都开）；需要验证"关闭后不发送"的用例再单独覆写
        lenient().when(notificationSettings.isEmailAllowed(any())).thenReturn(true);
    }

    // ======================== auditPass 测试 ========================

    @Nested
    @DisplayName("审核通过 - auditPass")
    class AuditPassTests {

        @Test
        @DisplayName("应当成功审核通过并发送邮件（无备注）")
        void shouldApproveAndSendEmailWithoutReason() {
            // Given
            BookingQueryView status = buildPendingStatus();
            when(bookingRepository.getServiceStatusByOrderId(VALID_ORDER_ID)).thenReturn(status);
            when(bookingRepository.auditService(eq(VALID_ORDER_ID), eq(ManageStatus.APPROVED.getCode()), isNull(),
                    eq(List.of(ManageStatus.SUBMIT))))
                    .thenReturn(true);
            when(bookingRepository.getUserEmailByOrderId(VALID_ORDER_ID)).thenReturn("test@example.com");

            // When
            assertDoesNotThrow(() -> serviceStatusService.auditPass(VALID_ORDER_ID, null, AuditSource.ADMIN));

            // Then
            verify(bookingRepository).auditService(eq(VALID_ORDER_ID), eq(ManageStatus.APPROVED.getCode()), isNull(),
                    eq(List.of(ManageStatus.SUBMIT)));
            verify(bookingRepository).getUserEmailByOrderId(VALID_ORDER_ID);
            verify(emailService).sendEmail(eq("test@example.com"), contains("通过"), anyString());
            // 审核通过必须发 APPROVED 事件，驱动 KB 失效问答/语义缓存
            verify(bookingEventPublisher)
                    .publishChanged(BOOKING_USER_ID, BOOKING_SERVICE_ID, "APPROVED");
        }

        @Test
        @DisplayName("应当成功审核通过并发送邮件（含备注）")
        void shouldApproveAndSendEmailWithReason() {
            // Given
            BookingQueryView status = buildPendingStatus();
            String reason = "预约信息完整，予以通过";
            when(bookingRepository.getServiceStatusByOrderId(VALID_ORDER_ID)).thenReturn(status);
            when(bookingRepository.auditService(eq(VALID_ORDER_ID), eq(ManageStatus.APPROVED.getCode()), eq(reason),
                    eq(List.of(ManageStatus.SUBMIT))))
                    .thenReturn(true);
            when(bookingRepository.getUserEmailByOrderId(VALID_ORDER_ID)).thenReturn("test@example.com");

            // When
            assertDoesNotThrow(() -> serviceStatusService.auditPass(VALID_ORDER_ID, reason, AuditSource.ADMIN));

            // Then
            verify(bookingRepository).auditService(eq(VALID_ORDER_ID), eq(ManageStatus.APPROVED.getCode()), eq(reason),
                    eq(List.of(ManageStatus.SUBMIT)));
            verify(emailService).sendEmail(eq("test@example.com"), contains("通过"), contains(reason));
        }

        @Test
        @DisplayName("订单不存在时应当抛出 STATUS_NOT_FOUND 异常")
        void shouldThrowExceptionWhenOrderNotFound() {
            // Given
            when(bookingRepository.getServiceStatusByOrderId(INVALID_ORDER_ID)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> serviceStatusService.auditPass(INVALID_ORDER_ID, null, AuditSource.ADMIN));
            assertEquals(BookErrorCode.STATUS_NOT_FOUND.getCode(), exception.getCode());
            verify(bookingRepository, never()).auditService(anyLong(), anyInt(), any(), anyList());
        }

        @Test
        @DisplayName("审核更新失败时应当抛出 AUDIT_FAILED 异常")
        void shouldThrowExceptionWhenAuditUpdateFails() {
            // Given
            BookingQueryView status = buildPendingStatus();
            when(bookingRepository.getServiceStatusByOrderId(VALID_ORDER_ID)).thenReturn(status);
            when(bookingRepository.auditService(eq(VALID_ORDER_ID), anyInt(), any(), anyList())).thenReturn(false);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> serviceStatusService.auditPass(VALID_ORDER_ID, null, AuditSource.ADMIN));
            assertEquals(BookErrorCode.AUDIT_FAILED.getCode(), exception.getCode());
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        }
    }

    // ======================== auditReject 测试 ========================

    @Nested
    @DisplayName("审核驳回 - auditReject")
    class AuditRejectTests {

        @Test
        @DisplayName("应当成功驳回并发送含拒绝原因的邮件")
        void shouldRejectAndSendEmailWithReason() {
            // Given
            BookingQueryView status = buildPendingStatus();
            String reason = "预约时间与其他安排冲突";
            when(bookingRepository.getServiceStatusByOrderId(VALID_ORDER_ID)).thenReturn(status);
            when(bookingRepository.auditService(eq(VALID_ORDER_ID), eq(ManageStatus.REJECTED.getCode()), eq(reason),
                    eq(List.of(ManageStatus.SUBMIT, ManageStatus.APPROVED))))
                    .thenReturn(true);
            when(bookingRepository.getUserEmailByOrderId(VALID_ORDER_ID)).thenReturn("student@example.com");

            // When
            assertDoesNotThrow(() -> serviceStatusService.auditReject(VALID_ORDER_ID, reason, AuditSource.ADMIN));

            // Then
            verify(bookingRepository).auditService(eq(VALID_ORDER_ID), eq(ManageStatus.REJECTED.getCode()), eq(reason),
                    eq(List.of(ManageStatus.SUBMIT, ManageStatus.APPROVED)));
            verify(emailService).sendEmail(eq("student@example.com"), contains("未通过"), contains(reason));
            // 审核拒绝回补余量/时段后必须发 REJECTED 事件，避免 KB 缓存继续返回过期可约答案
            verify(bookingEventPublisher)
                    .publishChanged(BOOKING_USER_ID, BOOKING_SERVICE_ID, "REJECTED");
        }

        @Test
        @DisplayName("驳回成功时按订单释放库存（仅通用类预约在 SQL 层命中）")
        void shouldReleaseStockOnReject() {
            // Given
            BookingQueryView status = buildPendingStatus();
            String reason = "预约信息不完整";
            when(bookingRepository.getServiceStatusByOrderId(VALID_ORDER_ID)).thenReturn(status);
            when(bookingRepository.auditService(eq(VALID_ORDER_ID), eq(ManageStatus.REJECTED.getCode()), eq(reason),
                    eq(List.of(ManageStatus.SUBMIT, ManageStatus.APPROVED))))
                    .thenReturn(true);

            // When
            assertDoesNotThrow(() -> serviceStatusService.auditReject(VALID_ORDER_ID, reason, AuditSource.ADMIN));

            // Then：按订单维度释放（是否真扣减由 SQL 中资源外键 IS NULL 判定，7.3.6），并释放咨询时段
            verify(bookingRepository).releaseStockByOrderId(VALID_ORDER_ID);
            verify(bookingRepository).releaseSlotByOrderId(VALID_ORDER_ID);
            verify(bookingRepository, never()).releaseStock(anyLong());
        }

        @Test
        @DisplayName("驳回原因为 null 时应当抛出 AUDIT_REASON_REQUIRED 异常")
        void shouldThrowExceptionWhenReasonIsNull() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> serviceStatusService.auditReject(VALID_ORDER_ID, null, AuditSource.ADMIN));
            assertEquals(BookErrorCode.AUDIT_REASON_REQUIRED.getCode(), exception.getCode());
            verify(bookingRepository, never()).auditService(anyLong(), anyInt(), any(), anyList());
        }

        @Test
        @DisplayName("驳回原因为空字符串时应当抛出 AUDIT_REASON_REQUIRED 异常")
        void shouldThrowExceptionWhenReasonIsEmpty() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> serviceStatusService.auditReject(VALID_ORDER_ID, "", AuditSource.ADMIN));
            assertEquals(BookErrorCode.AUDIT_REASON_REQUIRED.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("驳回原因为纯空格时应当抛出 AUDIT_REASON_REQUIRED 异常")
        void shouldThrowExceptionWhenReasonIsBlank() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> serviceStatusService.auditReject(VALID_ORDER_ID, "   ", AuditSource.ADMIN));
            assertEquals(BookErrorCode.AUDIT_REASON_REQUIRED.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("驳回时订单不存在应当抛出 STATUS_NOT_FOUND 异常")
        void shouldThrowExceptionWhenOrderNotFoundOnReject() {
            // Given
            when(bookingRepository.getServiceStatusByOrderId(INVALID_ORDER_ID)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> serviceStatusService.auditReject(INVALID_ORDER_ID, "有原因但订单不存在", AuditSource.ADMIN));
            assertEquals(BookErrorCode.STATUS_NOT_FOUND.getCode(), exception.getCode());
            verify(bookingRepository, never()).auditService(anyLong(), anyInt(), any(), anyList());
        }
    }

    // ======================== 管理员强制取消/完结（3.4 僵尸单兜底） ========================

    @Nested
    @DisplayName("管理员强制取消 - adminForceCancel")
    class AdminForceCancelTests {

        @Test
        @DisplayName("已通过单强制取消成功：调用仓储释放占用并发送通知")
        void shouldForceCancelApprovedBooking() {
            // Given：用户侧无法取消的已通过通用单
            when(bookingRepository.getServiceStatusByOrderId(VALID_ORDER_ID)).thenReturn(buildApprovedStatus());
            when(bookingRepository.adminCancelAndRelease(VALID_ORDER_ID, null)).thenReturn(true);
            when(bookingRepository.getUserEmailByOrderId(VALID_ORDER_ID)).thenReturn("test@example.com");

            // When
            assertDoesNotThrow(() -> serviceStatusService.adminForceCancel(VALID_ORDER_ID, null));

            // Then
            verify(bookingRepository).adminCancelAndRelease(VALID_ORDER_ID, null);
            verify(emailService).sendEmail(eq("test@example.com"), contains("取消"), anyString());
            // 3.5：强制取消按订单真实归属发布 CANCELLED 事件
            verify(bookingEventPublisher)
                    .publishChanged(BOOKING_USER_ID, BOOKING_SERVICE_ID, "CANCELLED");
        }

        @Test
        @DisplayName("订单已是终态（UPDATE 0 行）应抛 AUDIT_FAILED，且不发邮件")
        void shouldThrowAuditFailedWhenAlreadyTerminal() {
            when(bookingRepository.getServiceStatusByOrderId(VALID_ORDER_ID)).thenReturn(buildApprovedStatus());
            when(bookingRepository.adminCancelAndRelease(eq(VALID_ORDER_ID), any())).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> serviceStatusService.adminForceCancel(VALID_ORDER_ID, "重复操作"));
            assertEquals(BookErrorCode.AUDIT_FAILED.getCode(), ex.getCode());
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
            // 终态 0 行：业务未生效，不得发布事件
            verify(bookingEventPublisher, never())
                    .publishChanged(anyLong(), anyLong(), anyString());
        }

        @Test
        @DisplayName("订单不存在应抛 STATUS_NOT_FOUND")
        void shouldThrowNotFoundWhenMissing() {
            when(bookingRepository.getServiceStatusByOrderId(INVALID_ORDER_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> serviceStatusService.adminForceCancel(INVALID_ORDER_ID, null));
            assertEquals(BookErrorCode.STATUS_NOT_FOUND.getCode(), ex.getCode());
            verify(bookingRepository, never()).adminCancelAndRelease(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("管理员强制完结 - adminForceComplete")
    class AdminForceCompleteTests {

        @Test
        @DisplayName("僵尸通用单强制完结成功：回补名额并通知用户")
        void shouldForceCompleteZombieBooking() {
            when(bookingRepository.getServiceStatusByOrderId(VALID_ORDER_ID)).thenReturn(buildApprovedStatus());
            when(bookingRepository.adminCompleteAndRelease(VALID_ORDER_ID, null)).thenReturn(true);
            when(bookingRepository.getUserEmailByOrderId(VALID_ORDER_ID)).thenReturn("test@example.com");

            assertDoesNotThrow(() -> serviceStatusService.adminForceComplete(VALID_ORDER_ID, null));

            verify(bookingRepository).adminCompleteAndRelease(VALID_ORDER_ID, null);
            verify(emailService).sendEmail(eq("test@example.com"), contains("完结"), anyString());
            // 3.5：强制完结发布 COMPLETED 事件（KB 侧统一按变更失效缓存）
            verify(bookingEventPublisher)
                    .publishChanged(BOOKING_USER_ID, BOOKING_SERVICE_ID, "COMPLETED");
        }

        @Test
        @DisplayName("带备注完结：备注透传仓储并出现在邮件中；空白备注归一为 null")
        void shouldPassReasonAndNormalizeBlank() {
            String reason = "活动已结束，人工核销";
            when(bookingRepository.getServiceStatusByOrderId(VALID_ORDER_ID)).thenReturn(buildApprovedStatus());
            when(bookingRepository.adminCompleteAndRelease(VALID_ORDER_ID, reason)).thenReturn(true);
            when(bookingRepository.getUserEmailByOrderId(VALID_ORDER_ID)).thenReturn("test@example.com");

            assertDoesNotThrow(() -> serviceStatusService.adminForceComplete(VALID_ORDER_ID, reason));
            verify(bookingRepository).adminCompleteAndRelease(VALID_ORDER_ID, reason);

            // 空白备注：归一化为 null，不写入 reason 列
            when(bookingRepository.getServiceStatusByOrderId(2L)).thenReturn(buildApprovedStatus());
            when(bookingRepository.adminCompleteAndRelease(2L, null)).thenReturn(true);
            assertDoesNotThrow(() -> serviceStatusService.adminForceComplete(2L, "   "));
            verify(bookingRepository).adminCompleteAndRelease(2L, null);
        }

        @Test
        @DisplayName("终态单完结应抛 AUDIT_FAILED")
        void shouldThrowAuditFailedWhenAlreadyTerminal() {
            when(bookingRepository.getServiceStatusByOrderId(VALID_ORDER_ID)).thenReturn(buildApprovedStatus());
            when(bookingRepository.adminCompleteAndRelease(eq(VALID_ORDER_ID), any())).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> serviceStatusService.adminForceComplete(VALID_ORDER_ID, null));
            assertEquals(BookErrorCode.AUDIT_FAILED.getCode(), ex.getCode());
            verify(bookingEventPublisher, never())
                    .publishChanged(anyLong(), anyLong(), anyString());
        }
    }

    // ======================== 辅助方法 ========================

    private BookingQueryView buildPendingStatus() {
        BookingQueryView status = new BookingQueryView();
        status.setOrderId(VALID_ORDER_ID);
        status.setUserId(BOOKING_USER_ID);
        status.setServiceId(BOOKING_SERVICE_ID);
        status.setUsername("测试用户");
        status.setServiceName("自习室预约");
        status.setServiceDescribe("图书馆自习室预约服务");
        status.setManageStatus(ManageStatus.SUBMIT.getCode());
        return status;
    }

    private BookingQueryView buildApprovedStatus() {
        BookingQueryView status = buildPendingStatus();
        status.setManageStatus(ManageStatus.APPROVED.getCode());
        return status;
    }
}
