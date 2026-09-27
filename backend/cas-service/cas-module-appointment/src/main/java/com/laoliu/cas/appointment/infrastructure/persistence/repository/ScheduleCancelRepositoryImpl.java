package com.laoliu.cas.appointment.infrastructure.persistence.repository;

import com.laoliu.cas.appointment.domain.entity.ScheduleCancelRequest;
import com.laoliu.cas.appointment.domain.repository.ScheduleCancelRepository;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ScheduleCancelRequestDO;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.ScheduleCancelRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author forever-king
 */
@Repository
@RequiredArgsConstructor
public class ScheduleCancelRepositoryImpl implements ScheduleCancelRepository {
    private final ScheduleCancelRequestMapper mapper;

    @Override
    public void insert(ScheduleCancelRequest request) {
        mapper.insert(request.getConsultantId(), request.getSlotId(), request.getSlotDate(),
                request.getStartTime(), request.getEndTime(), request.getReason(), request.getTeacherUserId());
    }

    @Override
    public List<ScheduleCancelRequest> findByTeacher(Long teacherUserId) {
        return mapper.findByTeacher(teacherUserId).stream().map(ScheduleCancelRequestDO::toEntity).toList();
    }

    @Override
    public List<ScheduleCancelRequest> findPending() {
        return mapper.findPending().stream().map(ScheduleCancelRequestDO::toEntity).toList();
    }

    @Override
    public Optional<ScheduleCancelRequest> findByIdForUpdate(Long id) {
        return Optional.ofNullable(mapper.selectByIdForUpdate(id)).map(ScheduleCancelRequestDO::toEntity);
    }

    @Override
    public int countPendingForSlot(Long slotId) {
        return mapper.countPendingForSlot(slotId);
    }

    @Override
    public Optional<ScheduleCancelRequest> findPendingBySlotForUpdate(Long slotId) {
        return Optional.ofNullable(mapper.selectPendingBySlotForUpdate(slotId)).map(ScheduleCancelRequestDO::toEntity);
    }

    @Override
    public boolean approve(Long id, Long auditorId, String remark) {
        return mapper.approve(id, auditorId, remark) == 1;
    }

    @Override
    public boolean reject(Long id, Long auditorId, String remark) {
        return mapper.reject(id, auditorId, remark) == 1;
    }
}
