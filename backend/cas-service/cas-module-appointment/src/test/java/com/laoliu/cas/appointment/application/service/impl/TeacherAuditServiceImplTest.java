package com.laoliu.cas.appointment.application.service.impl;

import com.laoliu.cas.appointment.application.service.ServiceStatusService;
import com.laoliu.cas.appointment.application.service.TeacherAuditService;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.common.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TeacherAuditService 单元测试 —— 归属校验：仅本人名下咨询档期可审
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("教师审核服务单元测试")
class TeacherAuditServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ServiceStatusService serviceStatusService;

    private TeacherAuditService teacherAuditService;

    @BeforeEach
    void setUp() {
        teacherAuditService = new TeacherAuditServiceImpl(bookingRepository, serviceStatusService);
    }

    @Nested
    @DisplayName("审核通过 - approve")
    class ApproveTests {

        @Test
        @DisplayName("本人名下咨询档期的申请可通过")
        void shouldApproveWhenOwns() {
            when(bookingRepository.selectConsultantOwnerByOrderId(100L)).thenReturn(7L);

            teacherAuditService.approve(7L, 100L, null);

            verify(serviceStatusService).auditPass(100L, null);
        }

        @Test
        @DisplayName("非本人档期的申请抛 403 越权")
        void shouldForbidWhenNotOwns() {
            when(bookingRepository.selectConsultantOwnerByOrderId(100L)).thenReturn(8L);

            assertThrows(ForbiddenException.class, () -> teacherAuditService.approve(7L, 100L, null));

            verify(serviceStatusService, never()).auditPass(100L, null);
        }

        @Test
        @DisplayName("无归属（非咨询档期）也抛 403")
        void shouldForbidWhenNoOwner() {
            when(bookingRepository.selectConsultantOwnerByOrderId(100L)).thenReturn(null);

            assertThrows(ForbiddenException.class, () -> teacherAuditService.approve(7L, 100L, null));

            verify(serviceStatusService, never()).auditPass(100L, null);
        }
    }

    @Nested
    @DisplayName("审核拒绝 - reject")
    class RejectTests {

        @Test
        @DisplayName("本人名下可拒绝，携带原因转发")
        void shouldRejectWhenOwns() {
            when(bookingRepository.selectConsultantOwnerByOrderId(100L)).thenReturn(7L);

            teacherAuditService.reject(7L, 100L, "该时段已有安排");

            verify(serviceStatusService).auditReject(100L, "该时段已有安排");
        }

        @Test
        @DisplayName("他人档期拒绝抛 403，不透传")
        void shouldForbidRejectWhenNotOwns() {
            when(bookingRepository.selectConsultantOwnerByOrderId(100L)).thenReturn(9L);

            assertThrows(ForbiddenException.class, () -> teacherAuditService.reject(7L, 100L, "原因"));

            verify(serviceStatusService, never()).auditReject(100L, "原因");
        }
    }
}
