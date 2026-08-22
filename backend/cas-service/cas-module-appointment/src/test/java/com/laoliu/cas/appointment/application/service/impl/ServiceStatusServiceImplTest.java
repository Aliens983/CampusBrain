package com.laoliu.cas.appointment.application.service.impl;

import com.laoliu.cas.appointment.application.service.ServiceStatusService;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;
import com.laoliu.cas.common.enums.ManageStatus;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.BookErrorCode;
import com.laoliu.cas.infra.application.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ServiceStatusServiceImpl 单元测试。
 * 使用 Given-When-Then 模式，覆盖审核通过/驳回的所有分支。
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

    private ServiceStatusService serviceStatusService;

    private static final Long VALID_ORDER_ID = 1L;
    private static final Long INVALID_ORDER_ID = 999L;

    @BeforeEach
    void setUp() {
        serviceStatusService = new ServiceStatusServiceImpl(bookingRepository, emailService);
    }

    // ======================== auditPass 测试 ========================

    @Nested
    @DisplayName("审核通过 - auditPass")
    class AuditPassTests {

        @Test
        @DisplayName("应当成功审核通过并发送邮件（无备注）")
        void shouldApproveAndSendEmailWithoutReason() {
            // Given
            ServiceStatusResponse status = buildPendingStatus();
            when(bookingRepository.getServiceStatusByOrderId(VALID_ORDER_ID)).thenReturn(status);
            when(bookingRepository.auditService(eq(VALID_ORDER_ID), eq(ManageStatus.APPROVED.getCode()), isNull()))
                    .thenReturn(true);
            when(bookingRepository.getUserEmailByOrderId(VALID_ORDER_ID)).thenReturn("test@example.com");

            // When
            assertDoesNotThrow(() -> serviceStatusService.auditPass(VALID_ORDER_ID, null));

            // Then
            verify(bookingRepository).auditService(eq(VALID_ORDER_ID), eq(ManageStatus.APPROVED.getCode()), isNull());
            verify(bookingRepository).getUserEmailByOrderId(VALID_ORDER_ID);
            verify(emailService).sendEmail(eq("test@example.com"), contains("通过"), anyString());
        }

        @Test
        @DisplayName("应当成功审核通过并发送邮件（含备注）")
        void shouldApproveAndSendEmailWithReason() {
            // Given
            ServiceStatusResponse status = buildPendingStatus();
            String reason = "预约信息完整，予以通过";
            when(bookingRepository.getServiceStatusByOrderId(VALID_ORDER_ID)).thenReturn(status);
            when(bookingRepository.auditService(eq(VALID_ORDER_ID), eq(ManageStatus.APPROVED.getCode()), eq(reason)))
                    .thenReturn(true);
            when(bookingRepository.getUserEmailByOrderId(VALID_ORDER_ID)).thenReturn("test@example.com");

            // When
            assertDoesNotThrow(() -> serviceStatusService.auditPass(VALID_ORDER_ID, reason));

            // Then
            verify(bookingRepository).auditService(eq(VALID_ORDER_ID), eq(ManageStatus.APPROVED.getCode()), eq(reason));
            verify(emailService).sendEmail(eq("test@example.com"), contains("通过"), contains(reason));
        }

        @Test
        @DisplayName("订单不存在时应当抛出 STATUS_NOT_FOUND 异常")
        void shouldThrowExceptionWhenOrderNotFound() {
            // Given
            when(bookingRepository.getServiceStatusByOrderId(INVALID_ORDER_ID)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> serviceStatusService.auditPass(INVALID_ORDER_ID, null));
            assertEquals(BookErrorCode.STATUS_NOT_FOUND.getCode(), exception.getCode());
            verify(bookingRepository, never()).auditService(anyLong(), anyInt(), any());
        }

        @Test
        @DisplayName("审核更新失败时应当抛出 AUDIT_FAILED 异常")
        void shouldThrowExceptionWhenAuditUpdateFails() {
            // Given
            ServiceStatusResponse status = buildPendingStatus();
            when(bookingRepository.getServiceStatusByOrderId(VALID_ORDER_ID)).thenReturn(status);
            when(bookingRepository.auditService(eq(VALID_ORDER_ID), anyInt(), any())).thenReturn(false);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> serviceStatusService.auditPass(VALID_ORDER_ID, null));
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
            ServiceStatusResponse status = buildPendingStatus();
            String reason = "预约时间与其他安排冲突";
            when(bookingRepository.getServiceStatusByOrderId(VALID_ORDER_ID)).thenReturn(status);
            when(bookingRepository.auditService(eq(VALID_ORDER_ID), eq(ManageStatus.REJECTED.getCode()), eq(reason)))
                    .thenReturn(true);
            when(bookingRepository.getUserEmailByOrderId(VALID_ORDER_ID)).thenReturn("student@example.com");

            // When
            assertDoesNotThrow(() -> serviceStatusService.auditReject(VALID_ORDER_ID, reason));

            // Then
            verify(bookingRepository).auditService(eq(VALID_ORDER_ID), eq(ManageStatus.REJECTED.getCode()), eq(reason));
            verify(emailService).sendEmail(eq("student@example.com"), contains("未通过"), contains(reason));
        }

        @Test
        @DisplayName("驳回原因为 null 时应当抛出 AUDIT_REASON_REQUIRED 异常")
        void shouldThrowExceptionWhenReasonIsNull() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> serviceStatusService.auditReject(VALID_ORDER_ID, null));
            assertEquals(BookErrorCode.AUDIT_REASON_REQUIRED.getCode(), exception.getCode());
            verify(bookingRepository, never()).auditService(anyLong(), anyInt(), any());
        }

        @Test
        @DisplayName("驳回原因为空字符串时应当抛出 AUDIT_REASON_REQUIRED 异常")
        void shouldThrowExceptionWhenReasonIsEmpty() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> serviceStatusService.auditReject(VALID_ORDER_ID, ""));
            assertEquals(BookErrorCode.AUDIT_REASON_REQUIRED.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("驳回原因为纯空格时应当抛出 AUDIT_REASON_REQUIRED 异常")
        void shouldThrowExceptionWhenReasonIsBlank() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> serviceStatusService.auditReject(VALID_ORDER_ID, "   "));
            assertEquals(BookErrorCode.AUDIT_REASON_REQUIRED.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("驳回时订单不存在应当抛出 STATUS_NOT_FOUND 异常")
        void shouldThrowExceptionWhenOrderNotFoundOnReject() {
            // Given
            when(bookingRepository.getServiceStatusByOrderId(INVALID_ORDER_ID)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> serviceStatusService.auditReject(INVALID_ORDER_ID, "有原因但订单不存在"));
            assertEquals(BookErrorCode.STATUS_NOT_FOUND.getCode(), exception.getCode());
            verify(bookingRepository, never()).auditService(anyLong(), anyInt(), any());
        }
    }

    // ======================== 辅助方法 ========================

    private ServiceStatusResponse buildPendingStatus() {
        ServiceStatusResponse status = new ServiceStatusResponse();
        status.setOrderId(VALID_ORDER_ID);
        status.setUserId(100L);
        status.setUsername("测试用户");
        status.setServiceName("自习室预约");
        status.setServiceDescribe("图书馆自习室预约服务");
        status.setManageStatus(ManageStatus.SUBMIT.getCode());
        return status;
    }
}
