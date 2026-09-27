package com.laoliu.cas.appointment.application.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

/**
 * @author forever-king
 */
@Value
@Builder
public class TeacherScheduleSlotResponse {
    Long slotId;
    Long consultantId;
    String consultantName;
    LocalDate date;
    String startTime;
    String endTime;
    int availability;
    Long pendingRequestId;
}
