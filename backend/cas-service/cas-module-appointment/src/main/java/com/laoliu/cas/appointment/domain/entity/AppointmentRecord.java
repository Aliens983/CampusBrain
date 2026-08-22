package com.laoliu.cas.appointment.domain.entity;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * 预约记录领域实体
 * 纯净，不依赖任何框架注解
 *
 * @author forever-king
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode()
@ToString
@Builder
public class AppointmentRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Integer recordId;

    /** 预约项ID */
    private Integer itemId;

    /** 用户ID */
    private Long userId;

    /** 预约时间 */
    private String appointmentTime;

    /** 预约地点 */
    private String appointmentPlace;

    /** 预约状态 */
    private Integer appointmentState;

    /** 描述 */
    private String description;

    /** 创建时间 */
    private String createTime;

}
