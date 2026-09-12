package com.kb.infrastructure.client.dto;

import lombok.Data;

/**
 * CAS 预约助手：设备（对应 AssistantEquipmentVO）
 *
 * @author forever-king
 */
@Data
public class CasEquipmentOption {

    private Long equipmentId;
    private String name;
    private String category;
    private String description;
    private Integer totalStock;
    private Integer availableStock;
    private String unit;
    private String location;
    private Long serviceId;
    private String serviceName;
    private String campus;
    private String campusName;
    private Integer remainingForWindow;
}
