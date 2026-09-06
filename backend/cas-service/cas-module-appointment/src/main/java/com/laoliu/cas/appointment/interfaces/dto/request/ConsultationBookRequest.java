package com.laoliu.cas.appointment.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 咨询时段预约请求
 *
 * @author forever-king
 */
@Data
@Schema(description = "咨询时段预约请求")
public class ConsultationBookRequest {

    @NotNull(message = "时段不能为空")
    @Schema(description = "时段ID（来自可用时段接口返回的 slotId）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long slotId;
}
