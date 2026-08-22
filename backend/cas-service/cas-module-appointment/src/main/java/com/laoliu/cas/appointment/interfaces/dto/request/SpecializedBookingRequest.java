package com.laoliu.cas.appointment.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 专项预约请求（会议室、设备、咨询）
 *
 * @author forever-king
 */
@Data
@Schema(description = "专项预约请求")
public class SpecializedBookingRequest {

    @Schema(description = "资源ID（会议室ID/设备ID/咨询师ID）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roomId;

    @Schema(description = "设备ID")
    private String equipmentId;

    @Schema(description = "咨询师ID")
    private String consultantId;

    @NotBlank(message = "预约日期不能为空")
    @Schema(description = "预约日期")
    private String date;

    @NotBlank(message = "开始时间不能为空")
    @Schema(description = "开始时间")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    @Schema(description = "结束时间")
    private String endTime;

    @NotBlank(message = "用途不能为空")
    @Schema(description = "用途/主题")
    private String purpose;

    @Schema(description = "备注")
    private String remarks;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "主题")
    private String subject;

    /**
     * 从请求中提取资源ID，按优先级 roomId > equipmentId > consultantId
     */
    public Long extractServiceId() {
        String idStr = roomId;
        if (idStr == null || idStr.isEmpty()) {
            idStr = equipmentId;
        }
        if (idStr == null || idStr.isEmpty()) {
            idStr = consultantId;
        }
        if (idStr == null || idStr.isEmpty()) {
            return null;
        }
        return Long.valueOf(idStr);
    }
}
