package com.laoliu.cas.appointment.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laoliu.cas.appointment.domain.entity.ServiceCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务业务分类数据对象 - MyBatis-Plus ORM
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("service_category")
public class ServiceCategoryDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类编码 */
    private String code;

    /** 分类中文名 */
    private String name;

    /** 展示排序 */
    private Integer sort;

    public ServiceCategory toEntity() {
        return ServiceCategory.builder()
                .id(id).code(code).name(name).sort(sort)
                .build();
    }
}
