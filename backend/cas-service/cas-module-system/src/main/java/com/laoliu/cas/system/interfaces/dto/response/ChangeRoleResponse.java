package com.laoliu.cas.system.interfaces.dto.response;

import com.laoliu.cas.system.domain.entity.User;
import com.laoliu.cas.system.interfaces.convert.UserConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色变更响应 VO
 *
 * @author forever-king
 */
@Data
@Schema(description = "角色变更响应")
public class ChangeRoleResponse {

    // 安全：仅承载 UserResponse 投影（无密码哈希），不得直接放领域实体 User
    @Schema(description = "用户信息")
    private UserResponse user;

    @Schema(description = "变更后的角色名称")
    private String role;

    public static ChangeRoleResponse of(User user, String role) {
        ChangeRoleResponse vo = new ChangeRoleResponse();
        vo.setUser(UserConvert.INSTANCE.convert(user));
        vo.setRole(role);
        return vo;
    }
}
