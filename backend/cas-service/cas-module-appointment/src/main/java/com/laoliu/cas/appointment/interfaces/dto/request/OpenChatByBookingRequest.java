package com.laoliu.cas.appointment.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 打开咨询会话请求（凭自己的咨询预约单）
 * <p>
 * 学生从「我的预约」详情进入沟通时使用：后端按 orderId 校验归属并解析出咨询教师。
 *
 * @author forever-king
 */
@Data
@Schema(description = "按预约单打开咨询会话请求")
public class OpenChatByBookingRequest {

    @NotNull(message = "请提供预约单ID")
    @Schema(description = "当前用户的咨询预约订单ID")
    private Long orderId;
}
