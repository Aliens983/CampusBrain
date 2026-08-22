package com.laoliu.cas.appointment.interfaces.controller.app;

import com.laoliu.cas.appointment.application.service.ServiceService;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceAvailabilityVO;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.common.security.SecurityFrameworkUtils;
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
     * 身份来源：优先用 JWT 登录用户（{@link SecurityFrameworkUtils#getLoginUserId()}），
     * 忽略前端传入的 {@code X-User-Id}，防止越权；仅当无 JWT 用户（KB 内网签名场景，
     * 该头经 {@code InternalAuthFilter} 签名校验绑定）时才信任 {@code X-User-Id}。
     * </p>
     */
    @Operation(summary = "获取当前用户的预约记录", description = "供 KB 智能助手查询用户自己的预约")
    @GetMapping("/mine")
    public CommonResult<List<ServiceStatusResponse>> getMyBookings(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        // 有 JWT 登录用户：以登录身份为准（忽略 header，防伪造 X-User-Id 越权）
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        Long userId = loginUserId != null ? loginUserId : headerUserId;
        if (userId == null) {
            return CommonResult.badRequest("缺少用户身份");
        }
        return CommonResult.success(bookingRepository.getServiceStatusByUserId(userId));
    }
}
