package com.kb.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * CAS 服务只读接口客户端（Feign + Nacos 服务发现）。
 * <p>
 * 通过内网签名头调用 CAS 的只读接口，用于 AI 助手实时查询预约数据。
 * 直连 CAS（不走网关），依赖 {@link CasFeignConfig} 注入 X-Internal-Sign 签名。
 * </p>
 *
 * @author forever-king
 */
@FeignClient(name = "cas-service", configuration = CasFeignConfig.class)
public interface CasClient {

    /**
     * 获取可预约服务及其实时预约数。
     */
    @GetMapping("/api/v1/appointments/availability")
    CasResult<List<CasAvailability>> getAvailability();

    /**
     * 获取当前登录用户的预约记录（X-User-Id 由 Feign 拦截器动态注入）。
     */
    @GetMapping("/api/v1/appointments/mine")
    CasResult<List<CasBooking>> getMyBookings();
}
