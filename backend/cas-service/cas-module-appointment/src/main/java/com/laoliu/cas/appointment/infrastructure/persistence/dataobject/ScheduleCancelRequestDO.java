package com.laoliu.cas.appointment.infrastructure.persistence.dataobject;

import com.laoliu.cas.appointment.domain.entity.ScheduleCancelRequest;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ScheduleCancelRequestDO {
    private Long id;
    private Long consultantId;
    private Long slotId;
    private LocalDate slotDate;
    private String startTime;
    private String endTime;
    private String reason;
    private Integer status;
    private Long teacherUserId;
    private Long auditorId;
    private String auditRemark;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
    private String consultantName;
    private String teacherName;

    public ScheduleCancelRequest toEntity() {
        return ScheduleCancelRequest.builder()
                .id(id).consultantId(consultantId).slotId(slotId).slotDate(slotDate)
                .startTime(startTime).endTime(endTime).reason(reason)
                .status(status == null ? 0 : status).teacherUserId(teacherUserId)
                .auditorId(auditorId).auditRemark(auditRemark).auditTime(auditTime)
                .createTime(createTime).consultantName(consultantName).teacherName(teacherName).build();
    }
}
