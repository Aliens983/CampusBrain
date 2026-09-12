package com.kb.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成预约草稿请求（对应 CAS 的 AssistantBookingDraftRequest）
 * <p>
 * 资源类型由传入的资源 ID 组合推断：consultantId+slotId → 咨询；roomId → 教室；
 * equipmentId → 设备；仅 serviceId → 通用/活动。
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CasBookingDraftRequest {

    private Long serviceId;
    private Long consultantId;
    private Long slotId;
    private Long roomId;
    private Long equipmentId;
    private Integer quantity;
    private String date;
    private String startTime;
    private String endTime;
    private String purpose;
}
