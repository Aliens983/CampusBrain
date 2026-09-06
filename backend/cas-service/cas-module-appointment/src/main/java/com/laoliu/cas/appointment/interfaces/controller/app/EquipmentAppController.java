package com.laoliu.cas.appointment.interfaces.controller.app;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.impl.EquipmentServiceImpl;
import com.laoliu.cas.appointment.interfaces.dto.request.EquipmentBookRequest;
import com.laoliu.cas.appointment.interfaces.dto.response.EquipmentResponse;
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
 * 设备查询/设备借用接口（用户）
 *
 * @author forever-king
 */
@Tag(name = "设备（用户）")
@RestController
@RequestMapping("/app/equipment")
@RequiredArgsConstructor
public class EquipmentAppController {

    private final EquipmentServiceImpl equipmentService;
    private final GetUserIdViaTokenApi getUserIdViaTokenApi;

    @Operation(summary = "获取设备列表（分页）", description = "分页获取设备，支持按名称/分类/所属服务筛选")
    @GetMapping
    public CommonResult<PageResult<EquipmentResponse>> getEquipment(
            @Valid PageParam pageParam,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long serviceId) {
        IPage<EquipmentResponse> page = equipmentService.getAvailableEquipment(
                pageParam.getPageNo(), pageParam.getPageSize(), name, category, serviceId);
        return CommonResult.success(PageResult.of(page));
    }

    @Operation(summary = "获取设备分类", description = "获取所有设备分类列表（从数据库去重）")
    @GetMapping("/categories")
    public CommonResult<List<String>> getCategories() {
        return CommonResult.success(equipmentService.getCategories());
    }

    @Operation(summary = "获取设备详情", description = "根据ID获取单个设备详细信息")
    @GetMapping("/{id}")
    public CommonResult<EquipmentResponse> getEquipmentDetail(@PathVariable Long id) {
        EquipmentResponse equipment = equipmentService.getEquipmentById(id);
        if (equipment == null) {
            return CommonResult.notFound("设备不存在");
        }
        return CommonResult.success(equipment);
    }

    @Operation(summary = "申请借用设备", description = "固定时间段借用（数量+日期+起止），管理员审核通过后占用，到点自动归还")
    @PostMapping("/{equipmentId}/book")
    public CommonResult<Void> bookEquipment(
            @Parameter(description = "设备ID", required = true) @PathVariable Long equipmentId,
            @Valid @RequestBody EquipmentBookRequest request) {
        Long userId = getUserIdViaTokenApi.getUserId();
        equipmentService.bookEquipment(userId, equipmentId, request);
        return CommonResult.success("借用申请已提交，等待管理员审核", null);
    }
}
