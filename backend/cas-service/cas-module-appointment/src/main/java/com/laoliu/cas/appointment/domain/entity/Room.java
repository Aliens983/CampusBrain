package com.laoliu.cas.appointment.domain.entity;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * 教室领域实体 — 一个教室在同一时间段只允许被一人预约
 *
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class Room implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 教室ID */
    private Long id;
    /** 教室名称 */
    private String name;
    /** 位置 */
    private String location;
    /** 容纳人数 */
    private Integer seats;
    /** 所属服务ID（空闲教室服务） */
    private Long serviceId;
}
