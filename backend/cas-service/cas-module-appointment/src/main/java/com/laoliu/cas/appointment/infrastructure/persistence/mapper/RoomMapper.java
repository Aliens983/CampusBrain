package com.laoliu.cas.appointment.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.RoomDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 教室 Mapper
 *
 * @author forever-king
 */
@Mapper
public interface RoomMapper extends BaseMapper<RoomDO> {

    @Select("SELECT * FROM room WHERE service_id = #{serviceId} ORDER BY id")
    List<RoomDO> findByServiceId(@Param("serviceId") Long serviceId);

    /** 行锁读取教室（预约事务内防并发抢同一教室同一时段） */
    @Select("SELECT * FROM room WHERE id = #{id} FOR UPDATE")
    RoomDO selectByIdForUpdate(@Param("id") Long id);
}
