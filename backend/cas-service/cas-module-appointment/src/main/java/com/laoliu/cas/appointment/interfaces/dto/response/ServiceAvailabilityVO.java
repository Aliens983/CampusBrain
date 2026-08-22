package com.laoliu.cas.appointment.interfaces.dto.response;

import lombok.Data;

/**
 * 服务实时可用性（供 KB 智能助手查询）。
 *
 * @author forever-king
 */
@Data
public class ServiceAvailabilityVO {

    private Long serviceId;

    private String serviceName;

    private String serviceDescribe;

    /** 当前有效预约数（排除已取消） */
    private Integer bookingCount;
}
