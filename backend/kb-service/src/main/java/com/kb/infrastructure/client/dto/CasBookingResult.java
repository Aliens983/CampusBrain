package com.kb.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 预约/取消操作结果（对应 CAS 的 AssistantBookingResult）
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CasBookingResult {

    private Long orderId;
    private String status;
    private String statusText;
    private String message;
    private String resourceType;
    private String resourceName;
    private String serviceName;
    private String campusName;
    private String date;
    private String startTime;
    private String endTime;
    private Integer quantity;
}
