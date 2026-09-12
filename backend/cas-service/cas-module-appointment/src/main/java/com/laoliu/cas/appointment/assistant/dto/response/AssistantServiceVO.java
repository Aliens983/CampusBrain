package com.laoliu.cas.appointment.assistant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 预约助手：可预约服务视图（带校区、分类与实时余量）
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "预约助手·可预约服务")
public class AssistantServiceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "服务ID")
    private Long serviceId;

    @Schema(description = "服务名称")
    private String serviceName;

    @Schema(description = "服务描述")
    private String serviceDescribe;

    @Schema(description = "校区编码 cq仓前 / xs下沙")
    private String campus;

    @Schema(description = "校区中文名")
    private String campusName;

    @Schema(description = "分类编码 teacher/equipment/space/activity")
    private String categoryCode;

    @Schema(description = "分类中文名")
    private String categoryName;

    @Schema(description = "总容量（-1=不限）")
    private Integer capacity;

    @Schema(description = "已预约数")
    private Integer bookedCount;

    @Schema(description = "剩余名额（-1=不限）")
    private Integer remaining;

    @Schema(description = "当前是否可预约")
    private Boolean bookable;

    @Schema(description = "不可预约原因（可预约时为空）")
    private String bookableReason;
}
