package com.laoliu.cas.appointment.application.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author forever-king
 */
@Value
@Builder
public class ScheduleCancelRequestResponse {
    Long id;
    Long consultantId;
    String consultantName;
    Long slotId;
    LocalDate date;
    String startTime;
    String endTime;
    String reason;
    int status;
    Long teacherUserId;
    String teacherName;
    Long auditorId;
    String auditRemark;
    LocalDateTime auditTime;
    LocalDateTime createTime;
}
