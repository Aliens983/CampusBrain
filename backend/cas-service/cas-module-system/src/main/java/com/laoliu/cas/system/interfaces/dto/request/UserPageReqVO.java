package com.laoliu.cas.system.interfaces.dto.request;

import com.laoliu.cas.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户列表分页请求 — 支持按姓名、邮箱模糊搜索，按角色筛选。
 *
 * @author forever-king
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户列表分页请求")
public class UserPageReqVO extends PageParam {

    @Schema(description = "用户名（模糊搜索）", example = "张三")
    private String name;

    @Schema(description = "邮箱（模糊搜索）", example = "@example.com")
    private String email;

    @Schema(description = "角色（0=普通用户, 1=管理员, 2=超级管理员, 不传=全部）", example = "0")
    private Integer role;

}
