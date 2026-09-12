package com.laoliu.cas.appointment.assistant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 预约助手：咨询师视图（校区由所属服务继承）
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "预约助手·咨询师")
public class AssistantConsultantVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "咨询师ID")
    private Long consultantId;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "职称")
    private String title;

    @Schema(description = "所属部门")
    private String department;

    @Schema(description = "简介/擅长领域")
    private String description;

    @Schema(description = "所属服务ID")
    private Long serviceId;

    @Schema(description = "所属服务名称")
    private String serviceName;

    @Schema(description = "校区编码 cq仓前 / xs下沙")
    private String campus;

    @Schema(description = "校区中文名")
    private String campusName;

    @Schema(description = "指定日期的可用时段数（未传 date 时为 null）")
    private Integer availableSlotCount;
}
