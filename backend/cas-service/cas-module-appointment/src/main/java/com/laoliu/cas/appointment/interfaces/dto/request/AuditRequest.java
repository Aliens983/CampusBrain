package com.laoliu.cas.appointment.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 服务预约审核请求
 *
 * @author forever-king
 */
@Data
@Schema(description = "服务预约审核请求")
public class AuditRequest {

    @NotNull(message = "订单ID不能为空")
    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderId;

    @NotNull(message = "审核状态不能为空")
    @Min(value = 1, message = "审核状态必须为1(通过)或2(拒绝)")
    @Max(value = 2, message = "审核状态必须为1(通过)或2(拒绝)")
    @Schema(description = "审核状态（1-通过，2-拒绝）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    @Schema(description = "审核原因（审核状态为拒绝时必填）", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String reason;
}
