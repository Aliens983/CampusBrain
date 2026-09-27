package com.laoliu.cas.appointment.domain.entity;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Value
@Builder
public class ScheduleCancelRequest {
    Long id;
    Long consultantId;
    Long slotId;
    LocalDate slotDate;
    String startTime;
    String endTime;
    String reason;
    int status;
    Long teacherUserId;
    Long auditorId;
    String auditRemark;
    LocalDateTime auditTime;
    LocalDateTime createTime;
    String consultantName;
    String teacherName;
}
