package com.laoliu.cas.appointment.infrastructure.persistence.dataobject;

import lombok.Data;

/**
 * 咨询可预约时段数据对象（只读查询结果）。
 *
 * @author forever-king
 */
@Data
public class TimeSlotDO {

    private String startTime;

    private String endTime;

    private Integer available;
}
