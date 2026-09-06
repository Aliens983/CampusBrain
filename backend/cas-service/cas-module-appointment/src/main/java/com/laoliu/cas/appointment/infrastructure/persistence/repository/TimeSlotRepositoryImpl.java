package com.laoliu.cas.appointment.infrastructure.persistence.repository;

import com.laoliu.cas.appointment.domain.entity.TimeSlot;
import com.laoliu.cas.appointment.domain.repository.TimeSlotRepository;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.TimeSlotDO;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.TimeSlotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
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
    public List<TimeSlot> findAvailable(Long consultantId, LocalDate date) {
        return timeSlotMapper.findAvailableByConsultantAndDate(consultantId, date).stream()
                .map(TimeSlotDO::toEntity)
                .collect(Collectors.toList());
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
