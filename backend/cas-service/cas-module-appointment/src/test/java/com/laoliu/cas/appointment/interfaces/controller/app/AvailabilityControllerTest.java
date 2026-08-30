package com.laoliu.cas.appointment.interfaces.controller.app;

import com.laoliu.cas.appointment.application.service.ServiceService;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.common.security.SecurityFrameworkUtils;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AvailabilityController 单元测试（重点：/appointments/mine 身份校验）
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AvailabilityController 单元测试")
class AvailabilityControllerTest {

    @Mock private ServiceService serviceService;
    @Mock private BookingRepository bookingRepository;

    private static final Long LOGIN_USER_ID = 100L;
    private static final Long FORGED_HEADER_ID = 999L;

    private AvailabilityController controller() {
        return new AvailabilityController(serviceService, bookingRepository);
    }

    @Nested
    @DisplayName("GET /appointments/mine 身份校验")
    class GetMyBookings {

        @Test
        @DisplayName("JWT 登录用户应使用登录身份，忽略伪造的 X-User-Id")
        void shouldUseLoginUserIdIgnoringForgedHeader() {
            try (MockedStatic<SecurityFrameworkUtils> mocked =
                         mockStatic(SecurityFrameworkUtils.class)) {
                mocked.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
                ServiceStatusResponse st = new ServiceStatusResponse();
                when(bookingRepository.getServiceStatusByUserId(LOGIN_USER_ID)).thenReturn(List.of(st));

                CommonResult<List<ServiceStatusResponse>> result =
                        controller().getMyBookings(FORGED_HEADER_ID);

                assertTrue(result.isSuccess());
                // 用的是登录用户 100，而不是伪造的 header 999
                verify(bookingRepository).getServiceStatusByUserId(LOGIN_USER_ID);
                verify(bookingRepository, never()).getServiceStatusByUserId(FORGED_HEADER_ID);
            }
        }

        @Test
        @DisplayName("无 JWT 登录用户（内网签名场景）时才信任 X-User-Id")
        void shouldTrustHeaderWhenNoJwtUser() {
            try (MockedStatic<SecurityFrameworkUtils> mocked =
                         mockStatic(SecurityFrameworkUtils.class)) {
                mocked.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(null);
                ServiceStatusResponse st = new ServiceStatusResponse();
                when(bookingRepository.getServiceStatusByUserId(FORGED_HEADER_ID)).thenReturn(List.of(st));

                CommonResult<List<ServiceStatusResponse>> result =
                        controller().getMyBookings(FORGED_HEADER_ID);

                assertTrue(result.isSuccess());
                verify(bookingRepository).getServiceStatusByUserId(FORGED_HEADER_ID);
            }
        }

        @Test
        @DisplayName("既无登录用户也无 X-User-Id 时应返回参数错误")
        void shouldRejectWhenNoIdentity() {
            try (MockedStatic<SecurityFrameworkUtils> mocked =
                         mockStatic(SecurityFrameworkUtils.class)) {
                mocked.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(null);

                CommonResult<List<ServiceStatusResponse>> result =
                        controller().getMyBookings(null);

                assertEquals(400, result.getCode());
                verify(bookingRepository, never()).getServiceStatusByUserId(anyLong());
            }
        }
    }
}
