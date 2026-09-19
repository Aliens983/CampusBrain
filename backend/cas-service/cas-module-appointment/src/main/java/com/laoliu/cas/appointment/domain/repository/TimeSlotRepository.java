package com.laoliu.cas.appointment.domain.repository;

import com.laoliu.cas.appointment.domain.entity.TimeSlot;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 咨询可预约时段仓储接口
 *
 * @author forever-king
 */
public interface TimeSlotRepository {

    /** 按ID查询时段 */
    Optional<TimeSlot> findById(Long id);

    /** 查询某咨询师某日仍可预约（available=1）的时段 */
    List<TimeSlot> findAvailable(Long consultantId, LocalDate date);

    /**
     * 批量统计某日各咨询师仍可预约时段数（4.7 N+1 收敛）。
     * 当天无可用时段的咨询师不出现在返回 Map 中，调用方按 0 兜底。
     */
    Map<Long, Integer> countAvailableByConsultants(Collection<Long> consultantIds, LocalDate date);

    /** 原子占用时段：仅当 available=1 时置 0，返回是否占用成功（防并发抢同一时段） */
    boolean occupy(Long slotId);

    /** 释放时段：置回可预约 */
    boolean release(Long slotId);
}
