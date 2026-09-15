package com.laoliu.cas.appointment.interfaces.controller.app;

import com.laoliu.cas.appointment.application.service.ServiceItemService;
import com.laoliu.cas.appointment.interfaces.convert.ServiceConvert;
import com.laoliu.cas.appointment.interfaces.dto.request.ServicePageRequest;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceResponse;
import com.laoliu.cas.common.api.GetUserIdViaTokenApi;
import com.laoliu.cas.common.pojo.PageParam;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.common.result.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务查询接口（扁平路径，供前端直接调用）
 *
 * @author forever-king
 */
@Tag(name = "服务查询（用户）")
@RestController
@RequestMapping("/app/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceItemService serviceService;
    private final GetUserIdViaTokenApi getUserIdViaTokenApi;

    @Operation(summary = "获取可预约服务列表（分页+筛选）", description = "分页查询服务，支持按名称模糊搜索和状态筛选")
    @GetMapping
    public CommonResult<PageResult<ServiceResponse>> getEnabledServices(@Valid ServicePageRequest req) {
        return CommonResult.success(ServiceConvert.INSTANCE.convertPage(serviceService.getAllServices(req)));
    }

    @Operation(summary = "根据ID获取服务详情", description = "获取单个服务的详细信息")
    @GetMapping("/{id}")
    public CommonResult<ServiceResponse> getServiceById(@PathVariable Long id) {
        return serviceService.getServiceById(id)
                .map(s -> CommonResult.success(ServiceConvert.INSTANCE.convert(s)))
                .orElse(CommonResult.notFound("服务不存在"));
    }

    @Operation(summary = "获取当前用户的预约服务列表（分页）", description = "分页获取当前登录用户预约的所有服务")
    @GetMapping("/mine")
    public CommonResult<PageResult<ServiceResponse>> getMyServices(@Valid PageParam pageParam) {
        Long userId = getUserIdViaTokenApi.getUserId();
        return CommonResult.success(ServiceConvert.INSTANCE.convertPage(
                serviceService.selectUserServices(userId, pageParam.getPageNo(), pageParam.getPageSize())));
    }
}
