package com.laoliu.cas.appointment.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 轮播图响应（管理端）
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "轮播图响应")
public class CarouselResponse {

    @Schema(description = "轮播图 ID")
    private Long id;

    @Schema(description = "图片 URL")
    private String imageUrl;

    @Schema(description = "排序（小在前）")
    private Integer sort;

    @Schema(description = "是否启用：1 是 0 否")
    private Integer enabled;
}
