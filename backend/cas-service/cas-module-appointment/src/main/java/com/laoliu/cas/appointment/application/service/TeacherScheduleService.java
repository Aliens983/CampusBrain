package com.laoliu.cas.appointment.application.service;

import com.laoliu.cas.appointment.application.dto.response.ScheduleCancelRequestResponse;
import com.laoliu.cas.appointment.application.dto.response.TeacherScheduleSlotResponse;

import java.time.LocalDate;
import java.util.List;

public interface TeacherScheduleService {
    List<TeacherScheduleSlotResponse> getMySchedule(Long teacherUserId, LocalDate from, LocalDate to);
    List<ScheduleCancelRequestResponse> getMyRequests(Long teacherUserId);
    void applyCancellation(Long teacherUserId, Long slotId, String reason);
    List<ScheduleCancelRequestResponse> getPendingRequests();
    void approve(Long requestId, Long auditorId, String remark);
    void reject(Long requestId, Long auditorId, String remark);
}
