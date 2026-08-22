package com.laoliu.cas.appointment.interfaces.controller.app;

import com.laoliu.cas.appointment.application.service.ServiceService;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceAvailabilityVO;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;
import com.laoliu.cas.common.result.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 预约实时数据查询接口（只读），供 KB 智能助手查询实时预约数据。
 *
 * @author forever-king
 */
@Tag(name = "预约实时数据（用户）")
@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AvailabilityController {

    private final ServiceService serviceService;
    private final BookingRepository bookingRepository;

    @Operation(summary = "获取可预约服务及实时预约数", description = "供 KB 智能助手查询实时预约数据")
    @GetMapping("/availability")
    public CommonResult<List<ServiceAvailabilityVO>> getAvailability() {
        Map<Long, Long> counts = bookingRepository.countBookingsByService();
        List<ServiceAvailabilityVO> result = serviceService.getAvailableServices().stream()
                .map(s -> {
                    ServiceAvailabilityVO vo = new ServiceAvailabilityVO();
                    vo.setServiceId(s.getServiceId());
                    vo.setServiceName(s.getServiceName());
                    vo.setServiceDescribe(s.getServiceDescribe());
                    vo.setBookingCount(counts.getOrDefault(s.getServiceId(), 0L).intValue());
                    return vo;
                })
                .toList();
        return CommonResult.success(result);
    }

    /**
     * 获取当前用户的预约记录（供 KB 智能助手查询"我的预约"）。
     * <p>
     * userId 取自 {@code X-User-Id} 头——该头由 KB 携带内网签名（HMAC 绑定 userId），
     * 经 {@code InternalAuthFilter} 校验后才放行，因此可信任。
     * </p>
     */
    @Operation(summary = "获取当前用户的预约记录", description = "供 KB 智能助手查询用户自己的预约（X-User-Id 头）")
    @GetMapping("/mine")
    public CommonResult<List<ServiceStatusResponse>> getMyBookings(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return CommonResult.badRequest("缺少用户身份");
        }
        return CommonResult.success(bookingRepository.getServiceStatusByUserId(userId));
    }
}
