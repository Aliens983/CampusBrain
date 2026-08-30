package com.kb.infrastructure.client;

import lombok.Data;

/**
 * CAS 服务实时可用性数据（对应 CAS 的 ServiceAvailabilityVO）
 *
 * @author forever-king
 */
@Data
public class CasAvailability {

    /** 服务 ID */
    private Long serviceId;

    /** 服务名称 */
    private String serviceName;

    /** 服务描述 */
    private String serviceDescribe;

    /** 当前有效预约数（排除已取消） */
    private Integer bookingCount;
}
