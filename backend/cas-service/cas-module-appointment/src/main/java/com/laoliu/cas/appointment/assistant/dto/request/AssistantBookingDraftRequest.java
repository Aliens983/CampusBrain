package com.laoliu.cas.appointment.assistant.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 预约助手：生成预约草稿请求
 * <p>
 * 资源类型由传入的资源 ID 组合推断（四选一，互斥）：
 * <ul>
 *   <li>{@code consultantId + slotId} → 教师咨询</li>
 *   <li>{@code roomId} → 教室空间</li>
 *   <li>{@code equipmentId} → 设备借用</li>
 *   <li>仅 {@code serviceId} → 通用 / 活动报名</li>
 * </ul>
 * 生成草稿只做校验与预览，不写预约表；确认后才真正下单。
 *
 * @author forever-king
 */
@Data
@Schema(description = "预约助手·生成预约草稿请求")
public class AssistantBookingDraftRequest {

    @Schema(description = "服务ID（必填）", example = "1")
    private Long serviceId;

    @Schema(description = "咨询师ID（教师咨询场景）", example = "3")
    private Long consultantId;

    @Schema(description = "咨询时段ID（教师咨询场景，来自时段接口 slotId）", example = "12")
    private Long slotId;

    @Schema(description = "教室ID（教室空间场景）", example = "5")
    private Long roomId;

    @Schema(description = "设备ID（设备借用场景）", example = "7")
    private Long equipmentId;

    @Schema(description = "借用数量（设备场景，默认 1）", example = "1")
    private Integer quantity;

    @Schema(description = "日期 yyyy-MM-dd（教室/设备/咨询场景必填）", example = "2026-09-12")
    private String date;

    @Schema(description = "开始时间 HH:mm（教室/设备场景必填）", example = "09:00")
    private String startTime;

    @Schema(description = "结束时间 HH:mm（教室/设备场景必填）", example = "10:00")
    private String endTime;

    @Schema(description = "用途/主题（可选）", example = "期末复习")
    private String purpose;
}
