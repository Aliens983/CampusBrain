package com.laoliu.cas.appointment.domain.entity;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 咨询师领域实体 — 纯净，不依赖框架注解。
 *
 * @author forever-king
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class Consultant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 咨询师ID */
    private Long id;
    /** 姓名 */
    private String name;
    /** 所属部门 */
    private String department;
    /** 职称 */
    private String title;
    /** 简介 */
    private String description;
    /** 评分 */
    private BigDecimal rating;
    /** 评价数量 */
    private Integer reviewCount;
    /** 头像地址 */
    private String avatarUrl;
    /** 关联服务ID */
    private Long serviceId;

    public boolean hasRatings() {
        return reviewCount != null && reviewCount > 0;
    }
}
