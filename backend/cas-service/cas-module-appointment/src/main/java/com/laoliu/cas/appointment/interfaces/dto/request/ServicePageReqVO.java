package com.laoliu.cas.appointment.interfaces.dto.request;

import com.laoliu.cas.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 服务列表分页请求 — 支持按名称模糊搜索、按状态筛选。
 *
 * @author forever-king
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "服务列表分页请求")
public class ServicePageReqVO extends PageParam {

    @Schema(description = "服务名称（模糊搜索）", example = "自习室")
    private String serviceName;

    @Schema(description = "服务状态（0=禁用, 1=启用, 不传=全部）", example = "1")
    private Integer serviceState;

}
