package com.kb.infrastructure.client.dto;

import lombok.Data;

/**
 * CAS 预约助手：教室（对应 AssistantRoomVO）
 *
 * @author forever-king
 */
@Data
public class CasRoomOption {

    private Long roomId;
    private String name;
    private String location;
    private Integer seats;
    private Long serviceId;
    private String serviceName;
    private String campus;
    private String campusName;
    private Boolean free;
}
