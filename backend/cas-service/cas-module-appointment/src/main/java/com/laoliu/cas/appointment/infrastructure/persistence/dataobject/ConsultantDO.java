package com.laoliu.cas.appointment.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laoliu.cas.appointment.domain.entity.Consultant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 咨询师数据对象
 *
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("consultant")
public class ConsultantDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String department;
    private String title;
    private String description;
    private BigDecimal rating;
    private Integer reviewCount;
    private String avatarUrl;
    private Long serviceId;

    public Consultant toEntity() {
        return Consultant.builder()
                .id(id).name(name).department(department).title(title)
                .description(description).rating(rating).reviewCount(reviewCount)
                .avatarUrl(avatarUrl).serviceId(serviceId)
                .build();
    }

    public static ConsultantDO fromEntity(Consultant entity) {
        if (entity == null) return null;
        return ConsultantDO.builder()
                .id(entity.getId()).name(entity.getName()).department(entity.getDepartment())
                .title(entity.getTitle()).description(entity.getDescription())
                .rating(entity.getRating()).reviewCount(entity.getReviewCount())
                .avatarUrl(entity.getAvatarUrl()).serviceId(entity.getServiceId())
                .build();
    }
}
