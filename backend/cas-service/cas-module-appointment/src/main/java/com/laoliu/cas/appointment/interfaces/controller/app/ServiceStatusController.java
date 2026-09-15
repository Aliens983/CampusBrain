package com.laoliu.cas.appointment.interfaces.controller.app;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.ServiceStatusService;
import com.laoliu.cas.appointment.interfaces.dto.request.ServiceStatusPageRequest;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;
import com.laoliu.cas.common.api.GetUserIdViaTokenApi;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.common.result.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务状态查询接口（扁平路径，供前端直接调用）
 *
 * @author forever-king
 */
@Tag(name = "预约状态（用户）")
@RestController
@RequestMapping("/app/bookings")
@RequiredArgsConstructor
public class ServiceStatusController {

    private final ServiceStatusService serviceStatusService;
    private final GetUserIdViaTokenApi getUserIdViaTokenApi;

    @Operation(summary = "获取当前用户的预约状态（分页+筛选）", description = "分页获取当前登录用户的预约记录，支持按审核状态和服务名称筛选")
    @GetMapping("/mine")
    public CommonResult<PageResult<ServiceStatusResponse>> getServiceStatusByUser(@Valid ServiceStatusPageRequest req) {
        // 不再手写 try-catch：异常直接抛给 GlobalExceptionHandler。
        // 此前 catch 后 throw new BusinessException(INTERNAL_ERROR) 且既不记日志也不传 cause，
        // 注释却写着"交给 GlobalExceptionHandler 统一处理"——实际正是吞异常，
        // 线上出现 NPE / SQL 异常时只剩一句"服务器内部错误"，无法定位。
        Long userId = getUserIdViaTokenApi.getUserId();
        if (userId == null) {
            return CommonResult.badRequest("无法获取用户信息，请重新登录");
        }
        IPage<ServiceStatusResponse> statusPage = serviceStatusService
                .getServiceStatusByUserIdWithDescription(userId, req);
        return CommonResult.success(PageResult.of(statusPage));
    }

}
