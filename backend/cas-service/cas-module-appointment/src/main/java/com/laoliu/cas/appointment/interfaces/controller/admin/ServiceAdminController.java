package com.laoliu.cas.appointment.interfaces.controller.admin;

import com.laoliu.cas.appointment.application.service.ServiceService;
import com.laoliu.cas.appointment.interfaces.convert.ServiceConvert;
import com.laoliu.cas.appointment.interfaces.dto.request.ServiceAddRequest;
import com.laoliu.cas.appointment.interfaces.dto.request.ServicePageReqVO;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceRespVO;
import com.laoliu.cas.appointment.interfaces.dto.response.UserServicesRespVO;
import com.laoliu.cas.common.annotation.RequireRole;
import com.laoliu.cas.common.pojo.PageParam;
import com.laoliu.cas.system.api.UserInfoApi;
import com.laoliu.cas.system.api.dto.UserInfoDTO;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.exception.code.ServiceErrorCode;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.common.result.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


/**
 * 管理员端服务管理接口
 *
 * @author forever-king
 */
@Tag(name = "服务管理（管理）")
@RestController
@RequestMapping("/admin/services")
@RequiredArgsConstructor
public class ServiceAdminController {

    private final ServiceService serviceService;
    private final UserInfoApi userInfoApi;

    @Operation(summary = "获取所有服务（分页+筛选）", description = "分页获取服务列表，支持按名称模糊搜索和状态筛选")
    @GetMapping
    @RequireRole({UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<PageResult<ServiceRespVO>> getService(@Valid ServicePageReqVO reqVO) {
        return CommonResult.success(ServiceConvert.INSTANCE.convertPage(serviceService.getAllServices(reqVO)));
    }

    @Operation(summary = "添加服务", description = "管理员添加新的服务项目，包含服务名称、描述和状态")
    @PostMapping
    @RequireRole(UserRoleEnum.ADMIN)
    public CommonResult<Void> addService(@Valid @RequestBody ServiceAddRequest serviceAddRequest) {
        boolean success = serviceService.addService(serviceAddRequest);
        if (success) {
            return CommonResult.success("添加服务成功", null);
        } else {
            return CommonResult.error(ServiceErrorCode.SERVICE_BOOK_FAILED);
        }
    }

    @Operation(summary = "更新服务", description = "管理员更新已有的服务项目信息")
    @PutMapping("/{id}")
    @RequireRole(UserRoleEnum.ADMIN)
    public CommonResult<Void> updateService(@PathVariable Long id, @Valid @RequestBody ServiceAddRequest request) {
        boolean success = serviceService.updateService(id, request);
        if (success) {
            return CommonResult.success("更新服务成功", null);
        } else {
            return CommonResult.notFound("服务不存在");
        }
    }

    @Operation(summary = "获取指定用户的所有已预约服务（分页）", description = "管理员根据用户ID分页查询该用户预约的所有服务详情")
    @GetMapping("/by-user")
    @RequireRole(UserRoleEnum.ADMIN)
    public CommonResult<UserServicesRespVO> getUserServices(
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId,
            @Valid PageParam pageParam) {
        UserInfoDTO userInfo = userInfoApi.getUserById(userId);
        if (userInfo == null) {
            return CommonResult.error(ServiceErrorCode.SERVICE_NOT_FOUND);
        }
        PageResult<ServiceRespVO> servicesPage = ServiceConvert.INSTANCE.convertPage(
                serviceService.selectUserServices(userId, pageParam.getPageNo(), pageParam.getPageSize()));
        UserServicesRespVO respVO = UserServicesRespVO.of(
                userInfo.getName(), userId, userInfo.getRole(), userInfo.getEmail(), servicesPage);
        return CommonResult.success(respVO);
    }
}
