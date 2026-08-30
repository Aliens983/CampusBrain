package com.laoliu.cas.appointment.domain.entity;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * 设备领域实体 — 纯净，不依赖框架注解
 *
 * @author forever-king
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class Equipment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备ID */
    private Long id;
    /** 设备名称 */
    private String name;
    /** 设备分类 */
    private String category;
    /** 设备描述 */
    private String description;
    /** 总库存 */
    private Integer totalStock;
    /** 可用库存 */
    private Integer availableStock;
    /** 单位 */
    private String unit;
    /** 存放位置 */
    private String location;
    /** 关联服务ID */
    private Long serviceId;
    /** 设备图片URL */
    private String imageUrl;

    public boolean isAvailable() {
        return availableStock != null && availableStock > 0;
    }
}
