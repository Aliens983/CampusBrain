package com.laoliu.cas.appointment.domain.repository;

import com.laoliu.cas.appointment.domain.entity.ScheduleCancelRequest;

import java.util.List;
import java.util.Optional;

/**
 * @author forever-king
 */
public interface ScheduleCancelRepository {
    void insert(ScheduleCancelRequest request);

    List<ScheduleCancelRequest> findByTeacher(Long teacherUserId);

    List<ScheduleCancelRequest> findPending();

    Optional<ScheduleCancelRequest> findByIdForUpdate(Long id);

    int countPendingForSlot(Long slotId);

    Optional<ScheduleCancelRequest> findPendingBySlotForUpdate(Long slotId);

    boolean approve(Long id, Long auditorId, String remark);

    boolean reject(Long id, Long auditorId, String remark);
}
