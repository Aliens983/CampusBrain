package com.laoliu.cas.appointment.carousel.controller;

import com.laoliu.cas.appointment.carousel.service.CarouselService;
import com.laoliu.cas.common.annotation.RequireRole;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.result.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 首页轮播图管理（管理端）
 *
 * @author forever-king
 */
@Tag(name = "轮播图管理")
@RestController
@RequestMapping("/admin/carousel")
@RequiredArgsConstructor
public class CarouselAdminController {

    private final CarouselService carouselService;

    @Operation(summary = "轮播图列表")
    @GetMapping
    @RequireRole({UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<java.util.List<com.laoliu.cas.appointment.carousel.dataobject.CarouselDO>> list() {
        return CommonResult.success(carouselService.listAll());
    }

    @Operation(summary = "上传轮播图", description = "上传图片并加入轮播（自动追加到末尾）")
    @PostMapping
    @RequireRole({UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<Void> add(@Parameter(description = "图片文件") @RequestParam("file") MultipartFile file) {
        carouselService.add(file);
        return CommonResult.success("轮播图已添加", null);
    }

    @Operation(summary = "删除轮播图")
    @DeleteMapping("/{id}")
    @RequireRole({UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<Void> delete(@PathVariable Long id) {
        carouselService.delete(id);
        return CommonResult.success("已删除", null);
    }

    @Operation(summary = "拖拽排序", description = "body 为排序后的 id 数组")
    @PostMapping("/reorder")
    @RequireRole({UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<Void> reorder(@RequestBody java.util.List<Long> ids) {
        carouselService.reorder(ids);
        return CommonResult.success("排序已保存", null);
    }
}
