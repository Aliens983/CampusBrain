package com.kb.infrastructure.client.dto;

import lombok.Data;

/**
 * CAS 预约助手：咨询师（对应 AssistantConsultantVO）
 *
 * @author forever-king
 */
@Data
public class CasConsultantOption {

    private Long consultantId;
    private String name;
    private String title;
    private String department;
    private String description;
    private Long serviceId;
    private String serviceName;
    private String campus;
    private String campusName;
    private Integer availableSlotCount;
}
