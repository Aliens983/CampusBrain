package com.laoliu.cas.appointment.infrastructure.persistence.mapper;

import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.TimeSlotDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 咨询可预约时段 Mapper
 *
 * @author forever-king
 */
@Mapper
public interface TimeSlotMapper {

    @Select("SELECT id, consultant_id AS consultantId, slot_date AS slotDate, " +
            "start_time AS startTime, end_time AS endTime, available " +
            "FROM time_slot WHERE id = #{id}")
    TimeSlotDO selectById(@Param("id") Long id);

    @Select("SELECT id, consultant_id AS consultantId, slot_date AS slotDate, " +
            "start_time AS startTime, end_time AS endTime, available " +
            "FROM time_slot WHERE consultant_id = #{consultantId} AND slot_date = #{date} AND available = 1 " +
            "ORDER BY start_time")
    List<TimeSlotDO> findAvailableByConsultantAndDate(@Param("consultantId") Long consultantId,
                                                      @Param("date") LocalDate date);

    /**
     * 批量统计某日各咨询师仍可预约（available=1）的时段数（4.7 N+1 收敛）：
     * 助手「查咨询师列表」此前对每位咨询师各发一次 findAvailable，现一条 GROUP BY 取回。
     * 当天无可预约时段的咨询师不在结果集中，调用方按 0 兜底。
     */
    @Select("""
            <script>
            SELECT consultant_id AS consultantId, COUNT(*) AS cnt
            FROM time_slot
            WHERE slot_date = #{date} AND available = 1
              AND consultant_id IN
              <foreach collection="consultantIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY consultant_id
            </script>
            """)
    List<SlotCountRow> countAvailableByConsultants(@Param("consultantIds") Collection<Long> consultantIds,
                                                   @Param("date") LocalDate date);

    /** 按咨询师聚合计数投影行（consultantId → 可预约时段数） */
    record SlotCountRow(Long consultantId, Integer cnt) {
    }

    /** 原子占用：仅当 available=1 时置 0，返回受影响行数（0 表示已被占用/停用） */
    @Update("UPDATE time_slot SET available = 0 WHERE id = #{id} AND available = 1")
    int occupy(@Param("id") Long id);

    /** 释放：置回可预约 */
    @Update("UPDATE time_slot SET available = 1 WHERE id = #{id}")
    int release(@Param("id") Long id);
}
