package com.laoliu.cas.appointment.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 教室响应 VO
 *
 * @author forever-king
 */
@Data
@Builder
@Schema(description = "教室信息")
public class RoomResponse {

    @Schema(description = "教室ID")
    private Long id;

    @Schema(description = "教室名称", example = "A栋 201 教室")
    private String name;

    @Schema(description = "位置")
    private String location;

    @Schema(description = "容纳人数")
    private Integer seats;
}
