package com.laoliu.cas.system.interfaces.controller.admin;

import com.laoliu.cas.common.annotation.RequireRole;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.system.application.service.RoleService;
import com.laoliu.cas.system.interfaces.dto.request.ChangeRoleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员端角色管理接口
 * <p>
 * 业务校验统一交由 Service 层处理，异常由 {@code GlobalExceptionHandler} 统一捕获
 * <p>
 * 「查询当前登录用户自己的角色」不属于管理端能力，位于 {@code GET /users/me/role}
 *
 * @author forever-king
 */
@Tag(name = "用户角色管理（管理）")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class RoleAdminController {

    private final RoleService roleService;

    @Operation(summary = "修改用户角色", description = "管理员修改指定用户的角色（0=普通用户, 1=管理员, 3=教师；不可设 2 超管）")
    @PutMapping("/role")
    @RequireRole({UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<String> changeRole(@Valid @RequestBody ChangeRoleRequest request) {
        roleService.setRoleById(request.getUserId(), request.getRole());
        String roleName = switch (request.getRole() == null ? 0 : request.getRole()) {
            case 1 -> "管理员";
            case 3 -> "教师";
            default -> "普通用户";
        };
        return CommonResult.success("角色修改成功", roleName);
    }
}
