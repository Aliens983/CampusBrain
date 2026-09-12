package com.laoliu.cas.appointment.assistant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 预约助手：设备视图（校区由所属服务继承）
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "预约助手·设备")
public class AssistantEquipmentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "设备ID")
    private Long equipmentId;

    @Schema(description = "设备名称")
    private String name;

    @Schema(description = "设备分类")
    private String category;

    @Schema(description = "设备描述")
    private String description;

    @Schema(description = "总库存")
    private Integer totalStock;

    @Schema(description = "可用库存")
    private Integer availableStock;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "存放位置")
    private String location;

    @Schema(description = "所属服务ID")
    private Long serviceId;

    @Schema(description = "所属服务名称")
    private String serviceName;

    @Schema(description = "校区编码 cq仓前 / xs下沙")
    private String campus;

    @Schema(description = "校区中文名")
    private String campusName;

    @Schema(description = "指定借用窗口内剩余可借数量（未传窗口参数为 null）")
    private Integer remainingForWindow;
}
