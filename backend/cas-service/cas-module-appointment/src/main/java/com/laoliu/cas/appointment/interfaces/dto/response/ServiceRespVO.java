package com.laoliu.cas.appointment.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 服务响应 VO
 *
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "服务信息响应")
public class ServiceRespVO implements Serializable {

    @Schema(description = "服务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long serviceId;

    @Schema(description = "服务名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "自习室预约")
    private String serviceName;

    @Schema(description = "服务描述", example = "图书馆自习室预约服务")
    private String serviceDescribe;

    @Schema(description = "服务状态（0=禁用, 1=启用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer serviceState;

    @Schema(description = "业务分类: teacher/equipment/space/exam/other", example = "teacher")
    private String category;

    @Schema(description = "校区: cq仓前 / xs下沙", example = "cq")
    private String campus;

    @Schema(description = "服务封面图URL")
    private String imageUrl;

    @Schema(description = "可预约容量（-1=不限）", example = "-1")
    private Integer capacity;

}
