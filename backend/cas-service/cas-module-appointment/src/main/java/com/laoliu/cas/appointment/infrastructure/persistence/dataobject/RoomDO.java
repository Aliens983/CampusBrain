package com.laoliu.cas.appointment.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laoliu.cas.appointment.domain.entity.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 教室数据对象
 *
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("room")
public class RoomDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String location;
    private Integer seats;
    private Long serviceId;

    public Room toEntity() {
        return Room.builder()
                .id(id).name(name).location(location).seats(seats).serviceId(serviceId)
                .build();
    }
}
