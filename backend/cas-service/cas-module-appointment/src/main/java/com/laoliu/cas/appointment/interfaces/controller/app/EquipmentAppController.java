package com.laoliu.cas.appointment.interfaces.controller.app;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.impl.EquipmentServiceImpl;
import com.laoliu.cas.appointment.interfaces.dto.response.EquipmentResponse;
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
 * 设备查询接口。
 *
 * @author forever-king
 */
@Tag(name = "设备查询")
@RestController
@RequestMapping("/app/equipment")
@RequiredArgsConstructor
public class EquipmentAppController {

    private final EquipmentServiceImpl equipmentService;

    @Operation(summary = "获取设备列表（分页）", description = "分页获取所有设备，支持按名称/分类筛选")
    @GetMapping
    public CommonResult<PageResult<EquipmentResponse>> getEquipment(
            @Valid PageParam pageParam,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category) {
        IPage<EquipmentResponse> page = equipmentService.getAvailableEquipment(
                pageParam.getPageNo(), pageParam.getPageSize(), name, category);
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
}
