package com.laoliu.cas.appointment.interfaces.controller.app;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.impl.ConsultationServiceImpl;
import com.laoliu.cas.appointment.interfaces.dto.response.ConsultantResponse;
import com.laoliu.cas.appointment.interfaces.dto.response.TimeSlotRespVO;
import com.laoliu.cas.common.pojo.PageParam;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.common.result.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 咨询查询接口。
 *
 * @author forever-king
 */
@Tag(name = "咨询查询")
@RestController
@RequestMapping("/app/consultations")
@RequiredArgsConstructor
public class ConsultationAppController {

    private final ConsultationServiceImpl consultationService;

    @Operation(summary = "获取咨询师列表（分页）", description = "分页获取所有咨询师，支持按名称/部门筛选")
    @GetMapping
    public CommonResult<PageResult<ConsultantResponse>> getConsultants(
            @Valid PageParam pageParam,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String department) {
        IPage<ConsultantResponse> page = consultationService.getAvailableConsultants(
                pageParam.getPageNo(), pageParam.getPageSize(), name, department);
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

    @Operation(summary = "获取可用时段", description = "获取指定咨询师的可用预约时段")
    @GetMapping("/{consultantId}/slots")
    public CommonResult<List<TimeSlotRespVO>> getAvailableTime(
            @PathVariable Long consultantId,
            @RequestParam String date) {
        return CommonResult.success(consultationService.getAvailableTimeSlots(consultantId, date));
    }
}
