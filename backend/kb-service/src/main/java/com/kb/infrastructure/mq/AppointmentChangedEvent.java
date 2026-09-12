package com.kb.infrastructure.mq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
 * 与 CAS 侧 {@code com.laoliu.cas.appointment.infrastructure.mq.AppointmentChangedEvent}
 * 结构保持一致；两个服务各自持有定义，避免引入跨服务依赖。
 * <p>
 * 用 {@code @JsonIgnoreProperties(ignoreUnknown = true)} 容忍 CAS 侧新增字段，
 * 否则对方加字段就会导致本侧反序列化失败。
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppointmentChangedEvent implements Serializable {

    /** 事件类型：BOOKED（预约）/ CANCELLED（取消） */
    private String eventType;

    private Long userId;

    private Long serviceId;

    private LocalDateTime occurredAt;
}
