package com.laoliu.cas.appointment.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author forever-king
 */
@Data
@Schema(description = "服务状态响应")
public class ServiceStatusResponse {

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "服务名称")
    private String serviceName;

    @Schema(description = "服务描述")
    private String serviceDescribe;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "请求状态（0-待审核，1-通过，2-拒绝，3-取消）")
    private Integer manageStatus;

    @Schema(description = "服务状态码描述")
    private String statusDescription;

    @Schema(description = "审核拒绝原因")
    private String reason;

    @Schema(description = "咨询师姓名（咨询时段预约时非空）")
    private String consultantName;

    @Schema(description = "咨询预约日期（yyyy-MM-dd）")
    private LocalDate slotDate;

    @Schema(description = "咨询时段开始 HH:mm")
    private String startTime;

    @Schema(description = "咨询时段结束 HH:mm")
    private String endTime;
}
