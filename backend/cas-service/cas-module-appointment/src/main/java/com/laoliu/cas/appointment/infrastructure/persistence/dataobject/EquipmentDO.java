package com.laoliu.cas.appointment.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laoliu.cas.appointment.domain.entity.Equipment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备数据对象
 *
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("equipment")
public class EquipmentDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    private String description;
    private Integer totalStock;
    private Integer availableStock;
    private String unit;
    private String location;
    private Long serviceId;
    private String imageUrl;

    public Equipment toEntity() {
        return Equipment.builder()
                .id(id).name(name).category(category).description(description)
                .totalStock(totalStock).availableStock(availableStock)
                .unit(unit).location(location).serviceId(serviceId).imageUrl(imageUrl)
                .build();
    }

    public static EquipmentDO fromEntity(Equipment entity) {
        if (entity == null) return null;
        return EquipmentDO.builder()
                .id(entity.getId()).name(entity.getName()).category(entity.getCategory())
                .description(entity.getDescription())
                .totalStock(entity.getTotalStock()).availableStock(entity.getAvailableStock())
                .unit(entity.getUnit()).location(entity.getLocation()).serviceId(entity.getServiceId())
                .imageUrl(entity.getImageUrl())
                .build();
    }
}
