package com.laoliu.cas.appointment.application.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ScheduleCancelAuditRequest {
    @Size(max = 255, message = "审批意见不能超过255个字符")
    private String remark;
}
