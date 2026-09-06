package com.laoliu.cas.appointment.carousel.controller;

import com.laoliu.cas.appointment.carousel.service.CarouselService;
import com.laoliu.cas.common.result.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页轮播图（用户）
 *
 * @author forever-king
 */
@Tag(name = "轮播图（用户）")
@RestController
@RequestMapping("/app/carousel")
@RequiredArgsConstructor
public class CarouselAppController {

    private final CarouselService carouselService;

    @Operation(summary = "首页轮播图", description = "返回启用的轮播图片 URL 列表")
    @GetMapping
    public CommonResult<List<String>> list() {
        return CommonResult.success(carouselService.listImages());
    }
}
