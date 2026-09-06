package com.laoliu.cas.appointment.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 服务业务分类响应 VO
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "服务业务分类")
public class ServiceCategoryRespVO implements Serializable {

    @Schema(description = "分类ID（services.category_id 引用）", example = "1")
    private Long id;

    @Schema(description = "分类编码", example = "teacher")
    private String code;

    @Schema(description = "分类中文名", example = "教师咨询")
    private String name;

    @Schema(description = "展示排序", example = "1")
    private Integer sort;
}
