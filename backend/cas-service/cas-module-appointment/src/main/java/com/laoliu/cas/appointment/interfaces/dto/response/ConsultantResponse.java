package com.laoliu.cas.appointment.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 咨询师响应
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "咨询师响应")
public class ConsultantResponse {

    @Schema(description = "咨询师ID")
    private Long id;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "职称")
    private String title;

    @Schema(description = "所属部门")
    private String department;

    @Schema(description = "专长领域")
    private List<String> expertise;

    @Schema(description = "评分")
    private Double rating;

    @Schema(description = "评价数")
    private Integer reviews;

    @Schema(description = "是否可预约")
    private Boolean available;

    @Schema(description = "下一个可用时段")
    private String nextSlot;

    @Schema(description = "头像")
    private String avatar;
}
