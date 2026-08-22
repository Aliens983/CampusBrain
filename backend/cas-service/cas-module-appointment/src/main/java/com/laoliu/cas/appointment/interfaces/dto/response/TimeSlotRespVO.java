package com.laoliu.cas.appointment.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 咨询可用时段响应 VO。
 *
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "可用时段响应")
public class TimeSlotRespVO implements Serializable {

    @Schema(description = "开始时间", example = "09:00")
    private String startTime;

    @Schema(description = "结束时间", example = "10:00")
    private String endTime;

    @Schema(description = "是否可用", example = "true")
    private String available;

}
