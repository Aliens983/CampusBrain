package com.laoliu.cas.system.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户预约记录响应 VO — 替代原来的 Map&lt;String, Object&gt;。
 *
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "预约记录响应")
public class BookingRecordRespVO implements Serializable {

    @Schema(description = "订单ID", example = "1001")
    private Long orderId;

    @Schema(description = "服务名称", example = "自习室预约")
    private String serviceName;

    @Schema(description = "服务描述", example = "图书馆自习室")
    private String serviceDescribe;

    @Schema(description = "预约人姓名", example = "张三")
    private String username;

    @Schema(description = "预约创建时间")
    private LocalDateTime createTime;

    @Schema(description = "预约更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "审核状态（0=待审核, 1=通过, 2=拒绝, 3=取消）", example = "1")
    private Integer manageStatus;

    @Schema(description = "状态描述", example = "已通过")
    private String statusDescription;

}
