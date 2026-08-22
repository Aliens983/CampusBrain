package com.laoliu.cas.system.interfaces.controller.admin;

import com.laoliu.cas.common.annotation.RequireRole;
import com.laoliu.cas.common.api.GetUserIdViaTokenApi;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.system.application.service.RoleService;
import com.laoliu.cas.system.interfaces.dto.request.ChangeRoleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员端角色管理接口。
 * <p>
 * 业务校验统一交由 Service 层处理，异常由 {@code GlobalExceptionHandler} 统一捕获。
 *
 * @author forever-king
 */
@Tag(name = "用户角色管理（管理）")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class RoleAdminController {

    private final RoleService roleService;
    private final GetUserIdViaTokenApi getUserIdViaTokenApi;

    @Operation(summary = "获取用户角色", description = "获取当前登录用户的角色信息")
    @GetMapping("/role")
    @RequireRole({UserRoleEnum.USER, UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<String> getRole() {
        Long userId = getUserIdViaTokenApi.getUserId();
        String role = roleService.getRoleByUserId(userId);
        return CommonResult.success("获取用户角色成功", role);
    }

    @Operation(summary = "修改用户角色", description = "管理员修改指定用户的角色（0=普通用户, 1=管理员）")
    @PutMapping("/role")
    @RequireRole({UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<String> changeRole(@Valid @RequestBody ChangeRoleRequest request) {
        roleService.setRoleById(request.getUserId(), request.getRole());
        String roleName = request.getRole() == 1 ? "管理员" : "普通用户";
        return CommonResult.success("角色修改成功", roleName);
    }
}
