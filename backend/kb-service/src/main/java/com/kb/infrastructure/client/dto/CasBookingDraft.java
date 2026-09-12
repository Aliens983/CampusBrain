package com.kb.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 预约草稿（对应 CAS 的 AssistantBookingDraft）
 * <p>
 * 草稿只是"待用户确认的预约意图"，CAS 侧不落预约表、只存 Redis 并设置 TTL；
 * 用户确认后才真正下单。
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CasBookingDraft {

    public static final String TYPE_SERVICE = "SERVICE";
    public static final String TYPE_CONSULTATION = "CONSULTATION";
    public static final String TYPE_ROOM = "ROOM";
    public static final String TYPE_EQUIPMENT = "EQUIPMENT";

    private String draftId;
    private Long userId;
    private String resourceType;
    private String resourceTypeName;
    private Long serviceId;
    private String serviceName;
    private String campus;
    private String campusName;
    private String categoryCode;
    private String categoryName;
    private Long resourceId;
    private String resourceName;
    private String date;
    private String startTime;
    private String endTime;
    private Integer quantity;
    private String purpose;
    private String summary;
    private String confirmPrompt;
    private Long expiresAt;
    private Boolean needAudit;
    private Boolean valid;
    private String invalidReason;
    private List<String> warnings;
}
