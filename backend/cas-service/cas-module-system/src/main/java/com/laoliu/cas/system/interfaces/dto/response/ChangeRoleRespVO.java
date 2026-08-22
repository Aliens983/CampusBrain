package com.laoliu.cas.system.interfaces.dto.response;

import com.laoliu.cas.system.domain.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色变更响应 VO。
 *
 * @author forever-king
 */
@Data
@Schema(description = "角色变更响应")
public class ChangeRoleRespVO {

    @Schema(description = "用户信息")
    private User user;

    @Schema(description = "变更后的角色名称")
    private String role;

    public static ChangeRoleRespVO of(User user, String role) {
        ChangeRoleRespVO vo = new ChangeRoleRespVO();
        vo.setUser(user);
        vo.setRole(role);
        return vo;
    }
}
