package com.laoliu.cas.appointment.assistant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 预约助手：下单/取消结果
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "预约助手·操作结果")
public class AssistantBookingResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "预约单号（取消场景为被取消单号）")
    private Long orderId;

    @Schema(description = "结果状态 PENDING待审核 / APPROVED已通过 / CANCELLED已取消")
    private String status;

    @Schema(description = "状态中文描述")
    private String statusText;

    @Schema(description = "面向用户的提示语")
    private String message;

    @Schema(description = "资源类型")
    private String resourceType;

    @Schema(description = "资源名称")
    private String resourceName;

    @Schema(description = "服务名称")
    private String serviceName;

    @Schema(description = "校区中文名")
    private String campusName;

    @Schema(description = "日期")
    private String date;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;

    @Schema(description = "数量")
    private Integer quantity;
}
