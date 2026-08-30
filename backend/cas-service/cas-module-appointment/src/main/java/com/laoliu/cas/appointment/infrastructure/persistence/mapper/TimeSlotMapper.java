package com.laoliu.cas.appointment.infrastructure.persistence.mapper;

import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.TimeSlotDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 咨询可预约时段 Mapper
 *
 * @author forever-king
 */
@Mapper
public interface TimeSlotMapper {

    @Select("SELECT start_time AS startTime, end_time AS endTime, available FROM time_slot " +
            "WHERE consultant_id = #{consultantId} AND slot_date = #{date} AND available = 1 " +
            "ORDER BY start_time")
    List<TimeSlotDO> findAvailableByConsultantAndDate(@Param("consultantId") Long consultantId,
                                                      @Param("date") String date);
}
