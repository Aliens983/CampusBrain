package com.laoliu.cas.appointment.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 预约服务请求 — 用于 POST /app/bookings。
 *
 * @author forever-king
 */
@Data
@Schema(description = "预约服务请求")
public class BookServiceRequest {

    @NotEmpty(message = "服务ID列表不能为空")
    @Size(max = 50, message = "单次最多预约50个服务")
    @Schema(description = "要预约的服务ID列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1, 2]")
    private List<Long> serviceIds;
}
