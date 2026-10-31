package com.laoliu.cas.appointment.interfaces.controller.app;

import com.laoliu.cas.appointment.application.dto.response.ServiceStatusResponse;
import com.laoliu.cas.appointment.application.service.ServiceStatusService;
import com.laoliu.cas.appointment.domain.view.BookingQueryView;
import com.laoliu.cas.common.api.GetUserIdViaTokenApi;
import com.laoliu.cas.common.result.CommonResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户预约详情接口：归属参数必须来自登录身份，查不到返回 404.
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户预约详情接口测试")
class ServiceStatusControllerTest {

    @Mock private ServiceStatusService serviceStatusService;
    @Mock private GetUserIdViaTokenApi getUserIdViaTokenApi;

    private ServiceStatusController controller;

    @BeforeEach
    void setUp() {
        controller = new ServiceStatusController(serviceStatusService, getUserIdViaTokenApi);
    }

    @Test
    @DisplayName("按登录用户 + 订单ID查询，返回完整预约详情")
    void shouldReturnOwnedBooking() {
        when(getUserIdViaTokenApi.getUserId()).thenReturn(100L);
        BookingQueryView view = new BookingQueryView();
        view.setOrderId(9L);
        view.setUserId(100L);
        view.setServiceName("心理辅导");
        when(serviceStatusService.getServiceStatusByUserIdAndOrderId(100L, 9L)).thenReturn(view);

        CommonResult<ServiceStatusResponse> result = controller.getMyBooking(9L);

        assertTrue(result.isSuccess());
        assertEquals(9L, result.getData().getOrderId());
        verify(serviceStatusService).getServiceStatusByUserIdAndOrderId(100L, 9L);
    }

    @Test
    @DisplayName("订单不存在或不属于当前用户时返回 404")
    void shouldReturnNotFoundWhenNotOwned() {
        when(getUserIdViaTokenApi.getUserId()).thenReturn(100L);
        when(serviceStatusService.getServiceStatusByUserIdAndOrderId(100L, 9L)).thenReturn(null);

        CommonResult<ServiceStatusResponse> result = controller.getMyBooking(9L);

        assertEquals(404, result.getCode());
    }
}
