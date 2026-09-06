package com.laoliu.cas.appointment.infrastructure.persistence.dataobject;

import com.laoliu.cas.appointment.domain.entity.TimeSlot;
import lombok.Data;

import java.time.LocalDate;

/**
 * 咨询可预约时段数据对象
 *
 * @author forever-king
 */
@Data
public class TimeSlotDO {

    private Long id;

    private Long consultantId;

    private LocalDate slotDate;

    private String startTime;

    private String endTime;

    /** 0=已被占/停用，1=可预约 */
    private Integer available;

    public TimeSlot toEntity() {
        return TimeSlot.builder()
                .id(id)
                .consultantId(consultantId)
                .slotDate(slotDate)
                .startTime(startTime)
                .endTime(endTime)
                .available(available != null && available == 1)
                .build();
    }
}
