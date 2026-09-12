package com.laoliu.cas.appointment.assistant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 预约助手：教室视图（校区由所属服务继承）
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "预约助手·教室")
public class AssistantRoomVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "教室ID")
    private Long roomId;

    @Schema(description = "教室名称")
    private String name;

    @Schema(description = "位置")
    private String location;

    @Schema(description = "容纳人数")
    private Integer seats;

    @Schema(description = "所属服务ID")
    private Long serviceId;

    @Schema(description = "所属服务名称")
    private String serviceName;

    @Schema(description = "校区编码 cq仓前 / xs下沙")
    private String campus;

    @Schema(description = "校区中文名")
    private String campusName;

    @Schema(description = "指定日期+时段是否空闲（未传时段参数为 null）")
    private Boolean free;
}
