package com.laoliu.cas.appointment.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ScheduleCancelApplyRequest {
    @NotNull(message = "缺少排班时段ID")
    private Long slotId;

    @NotBlank(message = "请填写停诊事由")
    @Size(max = 255, message = "停诊事由不能超过255个字符")
    private String reason;
}
