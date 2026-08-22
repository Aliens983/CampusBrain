package com.laoliu.cas.system.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "用户信息响应")
public class UserResponse implements Serializable {

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String name;

    @Schema(description = "年级", example = "大一")
    private String grade;

    @Schema(description = "性别", example = "男")
    private String sex;

    @Schema(description = "年龄", example = "18")
    private Integer age;

    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "user@example.com")
    private String email;

    @Schema(description = "角色（1-普通用户，2-管理员，3-超级管理员）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer role;
}
