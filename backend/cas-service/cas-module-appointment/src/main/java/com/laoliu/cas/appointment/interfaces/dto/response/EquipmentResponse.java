package com.laoliu.cas.appointment.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 设备资源响应
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设备资源响应")
public class EquipmentResponse {

    @Schema(description = "设备ID")
    private Long id;

    @Schema(description = "设备名称")
    private String name;

    @Schema(description = "设备分类")
    private String category;

    @Schema(description = "设备描述")
    private String description;

    @Schema(description = "总库存")
    private Integer stock;

    @Schema(description = "可用库存")
    private Integer availableStock;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "价格标签")
    private String priceLabel;

    @Schema(description = "存放位置")
    private String location;

    @Schema(description = "图片")
    private String image;
}
