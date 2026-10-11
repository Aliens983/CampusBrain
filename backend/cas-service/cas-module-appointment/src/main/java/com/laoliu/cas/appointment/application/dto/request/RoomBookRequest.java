package com.laoliu.cas.appointment.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 教室时段预约请求（一个教室同一时间段只允许一人）
 *
 * @author forever-king
 */
@Data
@Schema(description = "教室时段预约请求")
public class RoomBookRequest {

    @NotBlank(message = "预约日期不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$",
            message = "预约日期格式必须为 yyyy-MM-dd")
    @Schema(description = "预约日期 yyyy-MM-dd", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-07")
    private String date;

    @NotBlank(message = "开始时间不能为空")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "开始时间格式必须为 HH:mm")
    @Schema(description = "开始时间 HH:mm", requiredMode = Schema.RequiredMode.REQUIRED, example = "09:00")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "结束时间格式必须为 HH:mm")
    @Schema(description = "结束时间 HH:mm", requiredMode = Schema.RequiredMode.REQUIRED, example = "12:00")
    private String endTime;
}
