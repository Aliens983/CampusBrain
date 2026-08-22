package com.laoliu.cas.system.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改用户角色请求
 *
 * @author forever-king
 */
@Data
@Schema(description = "修改用户角色请求")
public class ChangeRoleRequest {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "目标用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @NotNull(message = "角色不能为空")
    @Min(value = 0, message = "角色值必须为0或1")
    @Max(value = 1, message = "角色值必须为0或1")
    @Schema(description = "新角色（0=普通用户, 1=管理员）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer role;
}
