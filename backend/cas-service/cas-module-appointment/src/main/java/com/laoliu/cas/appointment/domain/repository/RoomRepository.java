package com.laoliu.cas.appointment.domain.repository;

import com.laoliu.cas.appointment.domain.entity.Room;

import java.util.List;
import java.util.Optional;

/**
 * 教室仓储接口
 *
 * @author forever-king
 */
public interface RoomRepository {

    Optional<Room> findById(Long id);

    /** 加行锁查询教室（防并发抢同一时段） */
    Optional<Room> findByIdForUpdate(Long id);

    List<Room> findByServiceId(Long serviceId);
}
