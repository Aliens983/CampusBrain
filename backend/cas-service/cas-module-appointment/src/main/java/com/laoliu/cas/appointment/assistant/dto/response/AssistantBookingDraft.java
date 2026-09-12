package com.laoliu.cas.appointment.assistant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 预约助手：预约草稿（待确认）
 * <p>
 * 草稿只是"待用户确认的预约意图"，不落预约表；
 * 存 Redis 并设置 TTL，确认时二次校验后走既有下单逻辑。
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "预约助手·预约草稿（待确认）")
public class AssistantBookingDraft implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 资源类型枚举值：SERVICE / CONSULTATION / ROOM / EQUIPMENT */
    public static final String TYPE_SERVICE = "SERVICE";
    public static final String TYPE_CONSULTATION = "CONSULTATION";
    public static final String TYPE_ROOM = "ROOM";
    public static final String TYPE_EQUIPMENT = "EQUIPMENT";

    @Schema(description = "草稿ID（确认时回传）")
    private String draftId;

    @Schema(description = "归属用户ID")
    private Long userId;

    @Schema(description = "资源类型 SERVICE/CONSULTATION/ROOM/EQUIPMENT")
    private String resourceType;

    @Schema(description = "资源类型中文名")
    private String resourceTypeName;

    @Schema(description = "服务ID")
    private Long serviceId;

    @Schema(description = "服务名称")
    private String serviceName;

    @Schema(description = "校区编码")
    private String campus;

    @Schema(description = "校区中文名")
    private String campusName;

    @Schema(description = "分类编码")
    private String categoryCode;

    @Schema(description = "分类中文名")
    private String categoryName;

    @Schema(description = "具体资源ID（咨询师/教室/设备；通用服务时为服务ID）")
    private Long resourceId;

    @Schema(description = "具体资源名称")
    private String resourceName;

    @Schema(description = "日期 yyyy-MM-dd")
    private String date;

    @Schema(description = "开始时间 HH:mm")
    private String startTime;

    @Schema(description = "结束时间 HH:mm")
    private String endTime;

    @Schema(description = "数量（设备借用）")
    private Integer quantity;

    @Schema(description = "用途/主题")
    private String purpose;

    @Schema(description = "一行摘要，用于向用户展示")
    private String summary;

    @Schema(description = "确认提示语")
    private String confirmPrompt;

    @Schema(description = "草稿过期时间（毫秒时间戳）")
    private Long expiresAt;

    @Schema(description = "确认后是否需要人工审核")
    private Boolean needAudit;

    @Schema(description = "校验是否通过")
    private Boolean valid;

    @Schema(description = "校验不通过原因（valid=false 时有效）")
    private String invalidReason;

    @Schema(description = "提示信息（如库存紧张）")
    private List<String> warnings;
}
