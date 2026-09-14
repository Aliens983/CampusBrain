package com.laoliu.cas.appointment.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * 首页轮播图领域实体
 * 纯净，不依赖任何框架注解
 *
 * @author forever-king
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class Carousel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 轮播图 ID */
    private Long id;

    /** 图片 URL */
    private String imageUrl;

    /** 排序（小在前） */
    private Integer sort;

    /** 是否启用：1 是 0 否 */
    private Integer enabled;
}
