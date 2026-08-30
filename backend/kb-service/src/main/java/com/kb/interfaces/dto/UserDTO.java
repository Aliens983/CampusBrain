package com.kb.interfaces.dto;

import com.kb.domain.user.KbUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 用户信息 DTO — 替代 Map&lt;String, Object&gt; 的强类型响应
 *
 * @author forever-king
 */
@Data
@Builder
@Schema(description = "用户信息")
public class UserDTO {

    @Schema(description = "用户 ID")
    private Long id;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "角色")
    private String role;
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "是否启用")
    private Boolean enabled;

    public static UserDTO from(KbUser user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail() != null ? user.getEmail() : "")
                .role(user.getRole())
                .nickname(user.getNickname() != null ? user.getNickname() : "")
                .enabled(user.getEnabled())
                .build();
    }
}
