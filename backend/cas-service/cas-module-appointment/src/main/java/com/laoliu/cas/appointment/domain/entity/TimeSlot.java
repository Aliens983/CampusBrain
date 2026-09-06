package com.laoliu.cas.appointment.domain.entity;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 咨询可预约时段领域实体 — 纯净，不依赖框架注解
 *
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class TimeSlot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 时段ID */
    private Long id;

    /** 所属咨询师ID */
    private Long consultantId;

    /** 日期 */
    private LocalDate slotDate;

    /** 开始时间 HH:mm */
    private String startTime;

    /** 结束时间 HH:mm */
    private String endTime;

    /** 是否可预约 */
    private boolean available;
}
