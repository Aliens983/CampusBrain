package com.laoliu.cas.appointment.infrastructure.persistence.repository;

import com.laoliu.cas.appointment.domain.entity.Room;
import com.laoliu.cas.appointment.domain.repository.RoomRepository;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.RoomDO;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.RoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 教室仓储实现
 *
 * @author forever-king
 */
@Repository
@RequiredArgsConstructor
public class RoomRepositoryImpl implements RoomRepository {

    private final RoomMapper roomMapper;

    @Override
    public Optional<Room> findById(Long id) {
        return Optional.ofNullable(roomMapper.selectById(id)).map(RoomDO::toEntity);
    }

    @Override
    public Optional<Room> findByIdForUpdate(Long id) {
        return Optional.ofNullable(roomMapper.selectByIdForUpdate(id)).map(RoomDO::toEntity);
    }

    @Override
    public List<Room> findByServiceId(Long serviceId) {
        return roomMapper.findByServiceId(serviceId).stream()
                .map(RoomDO::toEntity)
                .collect(Collectors.toList());
    }
}
