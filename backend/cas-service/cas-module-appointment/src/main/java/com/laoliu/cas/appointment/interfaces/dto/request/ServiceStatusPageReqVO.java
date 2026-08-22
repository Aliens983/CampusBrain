package com.laoliu.cas.appointment.interfaces.dto.request;

import com.laoliu.cas.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 预约状态分页请求 — 支持按审核状态、服务名称筛选。
 *
 * @author forever-king
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "预约状态分页请求")
public class ServiceStatusPageReqVO extends PageParam {

    @Schema(description = "审核状态（0=待审核, 1=通过, 2=拒绝, 3=取消, 不传=全部）", example = "0")
    private Integer manageStatus;

    @Schema(description = "服务名称（模糊搜索）", example = "自习室")
    private String serviceName;

}
