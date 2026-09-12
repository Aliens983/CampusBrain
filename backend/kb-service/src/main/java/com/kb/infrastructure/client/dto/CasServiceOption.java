package com.kb.infrastructure.client.dto;

import lombok.Data;

/**
 * CAS 预约助手：可预约服务（对应 AssistantServiceVO）
 *
 * @author forever-king
 */
@Data
public class CasServiceOption {

    private Long serviceId;
    private String serviceName;
    private String serviceDescribe;
    private String campus;
    private String campusName;
    private String categoryCode;
    private String categoryName;
    private Integer capacity;
    private Integer bookedCount;
    private Integer remaining;
    private Boolean bookable;
    private String bookableReason;
}
