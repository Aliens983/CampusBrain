package com.laoliu.cas.appointment.application.service.impl;

import com.laoliu.cas.appointment.domain.service.BookingWindowPolicy;
import com.laoliu.cas.appointment.application.service.RoomService;
import com.laoliu.cas.appointment.domain.entity.Room;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.domain.repository.RoomRepository;
import com.laoliu.cas.appointment.domain.repository.ServiceItemRepository;
import com.laoliu.cas.appointment.infrastructure.mq.BookingEventPublisher;
import com.laoliu.cas.appointment.application.dto.request.RoomBookRequest;
import com.laoliu.cas.appointment.application.dto.response.RoomResponse;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.BookErrorCode;
import com.laoliu.cas.common.exception.code.ServiceErrorCode;
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
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final ServiceItemRepository serviceRepository;
    private final BookingRepository bookingRepository;
    private final BookingEventPublisher bookingEventPublisher;

    /** 某服务（空闲教室）下的教室列表 */
    @Override
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
     *
     * @return 新预约单 orderId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long bookRoom(Long userId, Long roomId, RoomBookRequest req) {
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
        // P1-06：此前只拒绝"早于今天"，导致可以对昨天、以及今天已开始的时段下单。
        // 统一交给时间窗策略：过去日期 + 今天已开始/已结束的时段一律拒绝。
        BookingWindowPolicy.assertRoomBookable(date, req.getStartTime());

        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new BusinessException(BookErrorCode.ROOM_NOT_FOUND));
        // 服务被下架后不再接受新预约：否则管理员下架"教室空间"服务后，
        // 用户仍可通过专用端点约到教室，且这些预约会照常出现在列表中
        assertServiceAvailable(room.getServiceId());

        int occupied = bookingRepository.countRoomOverlap(roomId, date, req.getStartTime(), req.getEndTime());
        if (occupied > 0) {
            throw new BusinessException(BookErrorCode.ROOM_OCCUPIED);
        }

        // orderId 由 insert 回填；null=重复提交
        Long orderId = bookingRepository.insertRoomBooking(
                userId, room.getServiceId(), roomId, date, req.getStartTime(), req.getEndTime());
        if (orderId == null) {
            throw new BusinessException(BookErrorCode.BOOKING_REPEATED);
        }
        bookingEventPublisher.publishChanged(userId, room.getServiceId(), "BOOKED");
        return orderId;
    }

    /**
     * 校验服务处于上架状态。
     * <p>
     * 通用下单（{@code /app/bookings}）会校验 isAvailable，但教室/设备/咨询三条资源专用
     * 端点此前都不校验——管理员下架服务后用户照样能约，且这些预约会照常出现在列表里。
     * 这里统一补齐，与通用下单口径一致。
     */
    private void assertServiceAvailable(Long serviceId) {
        if (serviceId == null) {
            return;
        }
        serviceRepository.findById(serviceId)
                .filter(com.laoliu.cas.appointment.domain.entity.ServiceItem::isAvailable)
                .orElseThrow(() -> new BusinessException(ServiceErrorCode.SERVICE_DISABLED, serviceId));
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
