package com.laoliu.cas.appointment.infrastructure.mq;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预约变更事件（CAS 发布 → KB 消费）。
 * <p>
 * 用 DTO + JSON 序列化替代此前手工拼接字符串的做法：
 * 手拼方式没有转义，一旦字段里出现引号/换行就会产出非法 JSON，
 * 且字段增减时消费端无法感知。
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppointmentChangedEvent implements Serializable {

    /** 事件类型：BOOKED（预约）/ CANCELLED（取消） */
    private String eventType;

    /** 发起预约的用户 ID */
    private Long userId;

    /** 关联的服务 ID */
    private Long serviceId;

    /** 事件发生时间 */
    private LocalDateTime occurredAt;
}
