package com.laoliu.cas.appointment.infrastructure.persistence.repository;

import com.laoliu.cas.appointment.domain.entity.TimeSlot;
import com.laoliu.cas.appointment.domain.repository.TimeSlotRepository;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.TimeSlotDO;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.TimeSlotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 咨询可预约时段仓储实现
 *
 * @author forever-king
 */
@Repository
@RequiredArgsConstructor
public class TimeSlotRepositoryImpl implements TimeSlotRepository {

    private final TimeSlotMapper timeSlotMapper;

    @Override
    public Optional<TimeSlot> findById(Long id) {
        return Optional.ofNullable(timeSlotMapper.selectById(id))
                .map(TimeSlotDO::toEntity);
    }

    @Override
    public Optional<Integer> findAvailability(Long id) {
        return Optional.ofNullable(timeSlotMapper.selectById(id)).map(TimeSlotDO::getAvailable);
    }

    @Override
    public Optional<Integer> findAvailabilityForUpdate(Long id) {
        return Optional.ofNullable(timeSlotMapper.selectAvailabilityForUpdate(id));
    }

    @Override
    public Optional<TimeSlot> findByIdForUpdate(Long id) {
        return Optional.ofNullable(timeSlotMapper.selectByIdForUpdate(id)).map(TimeSlotDO::toEntity);
    }

    @Override
    public List<TimeSlot> findByConsultantsAndDateRange(Collection<Long> consultantIds, LocalDate startDate, LocalDate endDate) {
        if (consultantIds == null || consultantIds.isEmpty()) return List.of();
        return timeSlotMapper.findByConsultantsAndDateRange(consultantIds, startDate, endDate).stream()
                .map(TimeSlotDO::toEntity).toList();
    }

    @Override
    public boolean insertIfMissing(Long consultantId, LocalDate date, String startTime, String endTime) {
        return timeSlotMapper.insertIfMissing(consultantId, date, startTime, endTime) > 0;
    }

    @Override
    public boolean markCancelledIfAvailable(Long slotId) {
        return timeSlotMapper.markCancelledIfAvailable(slotId) == 1;
    }

    @Override
    public List<TimeSlot> findAvailable(Long consultantId, LocalDate date) {
        return timeSlotMapper.findAvailableByConsultantAndDate(consultantId, date).stream()
                .map(TimeSlotDO::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, Integer> countAvailableByConsultants(Collection<Long> consultantIds, LocalDate date) {
        if (consultantIds == null || consultantIds.isEmpty()) {
            return Map.of();
        }
        return timeSlotMapper.countAvailableByConsultants(consultantIds, date).stream()
                .collect(Collectors.toMap(TimeSlotMapper.SlotCountRow::consultantId,
                        TimeSlotMapper.SlotCountRow::cnt));
    }

    @Override
    public boolean occupy(Long slotId) {
        return timeSlotMapper.occupy(slotId) > 0;
    }

    @Override
    public boolean release(Long slotId) {
        return timeSlotMapper.release(slotId) > 0;
    }
}
