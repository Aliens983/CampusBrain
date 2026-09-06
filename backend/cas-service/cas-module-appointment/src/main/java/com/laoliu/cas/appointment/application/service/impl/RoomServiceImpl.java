package com.laoliu.cas.appointment.application.service.impl;

import com.laoliu.cas.appointment.domain.entity.Room;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.domain.repository.RoomRepository;
import com.laoliu.cas.appointment.infrastructure.mq.BookingEventPublisher;
import com.laoliu.cas.appointment.interfaces.dto.request.RoomBookRequest;
import com.laoliu.cas.appointment.interfaces.dto.response.RoomResponse;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.BookErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 教室查询/教室时段预约应用服务实现
 *
 * @author forever-king
 */
@Service
@RequiredArgsConstructor
public class RoomServiceImpl {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final BookingEventPublisher bookingEventPublisher;

    /** 某服务（空闲教室）下的教室列表 */
    public List<RoomResponse> listByService(Long serviceId) {
        if (serviceId == null) {
            return List.of();
        }
        return roomRepository.findByServiceId(serviceId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 教室时段预约：一个教室同一时间段只允许一人。
     * 并发安全：事务内对教室行加锁，再校验"该教室该时段无其它活跃预约"，才幂等落单。
     */
    @Transactional(rollbackFor = Exception.class)
    public void bookRoom(Long userId, Long roomId, RoomBookRequest req) {
        if (req == null || req.getStartTime() == null || req.getEndTime() == null
                || req.getStartTime().compareTo(req.getEndTime()) >= 0) {
            throw new BusinessException(BookErrorCode.BOOK_TIME_INVALID);
        }
        LocalDate date;
        try {
            date = LocalDate.parse(req.getDate());
        } catch (Exception e) {
            throw new BusinessException(BookErrorCode.BOOK_TIME_INVALID);
        }
        if (date.isBefore(LocalDate.now())) {
            throw new BusinessException(BookErrorCode.BOOK_TIME_INVALID);
        }

        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new BusinessException(BookErrorCode.ROOM_NOT_FOUND));

        int occupied = bookingRepository.countRoomOverlap(roomId, date, req.getStartTime(), req.getEndTime());
        if (occupied > 0) {
            throw new BusinessException(BookErrorCode.ROOM_OCCUPIED);
        }

        int inserted = bookingRepository.insertRoomBooking(
                userId, room.getServiceId(), roomId, date, req.getStartTime(), req.getEndTime());
        if (inserted == 0) {
            throw new BusinessException(BookErrorCode.BOOKING_REPEATED);
        }
        bookingEventPublisher.publishChanged(userId, room.getServiceId(), "BOOKED");
    }

    private RoomResponse toResponse(Room r) {
        return RoomResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .location(r.getLocation())
                .seats(r.getSeats())
                .build();
    }
}
