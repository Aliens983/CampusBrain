package com.laoliu.cas.appointment.interfaces.controller.app;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.impl.ConsultationServiceImpl;
import com.laoliu.cas.appointment.interfaces.dto.request.ConsultationBookRequest;
import com.laoliu.cas.appointment.interfaces.dto.response.ConsultantResponse;
import com.laoliu.cas.appointment.interfaces.dto.response.TimeSlotRespVO;
import com.laoliu.cas.common.api.GetUserIdViaTokenApi;
import com.laoliu.cas.common.pojo.PageParam;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.common.result.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 咨询查询与咨询时段预约接口（用户）
 *
 * @author forever-king
 */
@Tag(name = "咨询（用户）")
@RestController
@RequestMapping("/app/consultations")
@RequiredArgsConstructor
public class ConsultationAppController {

    private final ConsultationServiceImpl consultationService;
    private final GetUserIdViaTokenApi getUserIdViaTokenApi;

    @Operation(summary = "获取咨询师列表（分页）", description = "分页获取咨询师，支持按名称/部门/所属服务筛选")
    @GetMapping
    public CommonResult<PageResult<ConsultantResponse>> getConsultants(
            @Valid PageParam pageParam,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Long serviceId) {
        IPage<ConsultantResponse> page = consultationService.getAvailableConsultants(
                pageParam.getPageNo(), pageParam.getPageSize(), name, department, serviceId);
        return CommonResult.success(PageResult.of(page));
    }

    @Operation(summary = "获取咨询师详情", description = "根据ID获取单个咨询师详细信息")
    @GetMapping("/{id}")
    public CommonResult<ConsultantResponse> getConsultant(@PathVariable Long id) {
        ConsultantResponse consultant = consultationService.getConsultantById(id);
        if (consultant == null) {
            return CommonResult.notFound("咨询师不存在");
        }
        return CommonResult.success(consultant);
    }

    @Operation(summary = "获取可用时段", description = "获取指定咨询师在指定日期的可预约时段")
    @GetMapping("/{consultantId}/slots")
    public CommonResult<List<TimeSlotRespVO>> getAvailableTime(
            @Parameter(description = "咨询师ID", required = true) @PathVariable Long consultantId,
            @Parameter(description = "日期 yyyy-MM-dd", required = true) @RequestParam String date) {
        return CommonResult.success(consultationService.getAvailableTimeSlots(consultantId, date));
    }

    @Operation(summary = "预约咨询时段", description = "占用该咨询师指定时段，生成一条待审核咨询预约")
    @PostMapping("/{consultantId}/book")
    public CommonResult<Void> bookConsultation(
            @Parameter(description = "咨询师ID", required = true) @PathVariable Long consultantId,
            @Valid @RequestBody ConsultationBookRequest request) {
        Long userId = getUserIdViaTokenApi.getUserId();
        consultationService.bookConsultation(userId, consultantId, request.getSlotId());
        return CommonResult.success("预约成功，等待管理员审核", null);
    }
}
