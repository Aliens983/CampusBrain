package com.kb.infrastructure.client.dto;

import lombok.Data;

/**
 * CAS 咨询可预约时段（对应 TimeSlotRespVO）
 *
 * @author forever-king
 */
@Data
public class CasTimeSlot {

    private Long slotId;
    private String startTime;
    private String endTime;
    private String available;
}
