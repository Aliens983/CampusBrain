package com.laoliu.cas.appointment.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 服务添加请求
 *
 * @author forever-king
 */
@Data
@Schema(description = "服务添加请求")
public class ServiceAddRequest {

    @NotBlank(message = "服务名称不能为空")
    @Size(max = 100, message = "服务名称不能超过100个字符")
    @Schema(description = "服务名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "图书馆预约")
    private String serviceName;

    @NotBlank(message = "服务描述不能为空")
    @Size(max = 500, message = "服务描述不能超过500个字符")
    @Schema(description = "服务描述", requiredMode = Schema.RequiredMode.REQUIRED, example = "预约图书馆座位或图书")
    private String serviceDescribe;

    @Min(value = 0, message = "服务状态必须为0(禁用)或1(启用)")
    @Max(value = 1, message = "服务状态必须为0(禁用)或1(启用)")
    @Schema(description = "服务状态（0-禁用，1-启用；缺省默认启用）", example = "1")
    private Integer serviceState;

    @Schema(description = "业务分类（teacher/equipment/space/activity/other；缺省 other）")
    private String category;

    @Schema(description = "校区（cq=仓前 / xs=下沙；缺省 cq）")
    private String campus;

    @Min(value = -1, message = "容量不能小于 -1（-1=不限）")
    @Schema(description = "可预约容量（-1=不限，缺省 -1）", example = "-1")
    private Integer capacity;

    @Schema(description = "服务封面图URL（上传返回的相对路径，可空）")
    private String imageUrl;
}
