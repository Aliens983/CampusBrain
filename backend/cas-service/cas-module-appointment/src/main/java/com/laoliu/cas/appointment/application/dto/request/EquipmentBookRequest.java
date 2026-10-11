package com.laoliu.cas.appointment.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 设备借用请求（固定时间段，到点自动归还）
 *
 * @author forever-king
 */
@Data
@Schema(description = "设备借用请求")
public class EquipmentBookRequest {

    @NotNull(message = "借用数量不能为空")
    @Min(value = 1, message = "借用数量至少为 1")
    @Schema(description = "借用数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer quantity;

    @NotBlank(message = "借用日期不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$",
            message = "借用日期格式必须为 yyyy-MM-dd")
    @Schema(description = "借用日期 yyyy-MM-dd", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-07")
    private String date;

    @NotBlank(message = "开始时间不能为空")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "开始时间格式必须为 HH:mm")
    @Schema(description = "开始时间 HH:mm", requiredMode = Schema.RequiredMode.REQUIRED, example = "13:00")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "结束时间格式必须为 HH:mm")
    @Schema(description = "结束时间 HH:mm", requiredMode = Schema.RequiredMode.REQUIRED, example = "20:00")
    private String endTime;
}
