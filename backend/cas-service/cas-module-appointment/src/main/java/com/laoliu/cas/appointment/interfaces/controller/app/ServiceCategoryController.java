package com.laoliu.cas.appointment.interfaces.controller.app;

import com.laoliu.cas.appointment.application.service.ServiceCategoryService;
import com.laoliu.cas.appointment.domain.entity.ServiceCategory;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceCategoryRespVO;
import com.laoliu.cas.common.result.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 服务业务分类查询接口（只读字典，前端新增服务下拉 / 展示名使用）
 *
 * @author forever-king
 */
@Tag(name = "服务业务分类（字典）")
@RestController
@RequestMapping("/app/service-categories")
@RequiredArgsConstructor
public class ServiceCategoryController {

    private final ServiceCategoryService serviceCategoryService;

    @Operation(summary = "获取全部业务分类", description = "固定 4 类：教师咨询/设备借用/教室空间/活动报名")
    @GetMapping
    public CommonResult<List<ServiceCategoryRespVO>> listCategories() {
        List<ServiceCategoryRespVO> list = serviceCategoryService.listCategories().stream()
                .map(this::toVO)
                .toList();
        return CommonResult.success(list);
    }

    private ServiceCategoryRespVO toVO(ServiceCategory c) {
        return ServiceCategoryRespVO.builder()
                .id(c.getId()).code(c.getCode()).name(c.getName()).sort(c.getSort())
                .build();
    }
}
