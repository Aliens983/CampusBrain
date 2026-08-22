package com.laoliu.cas.appointment.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约记录响应 DTO
 *
 * @author forever-king
 */
@Data
@Schema(description = "预约记录响应")
public class BookingDTO {

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "服务名称")
    private String serviceName;

    @Schema(description = "预约状态（0=待审核, 1=通过, 2=拒绝, 3=取消）")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDescription;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "审核原因/备注")
    private String reason;
}
