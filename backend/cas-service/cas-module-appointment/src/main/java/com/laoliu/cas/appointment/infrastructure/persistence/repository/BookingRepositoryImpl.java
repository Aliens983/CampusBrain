package com.laoliu.cas.appointment.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.infrastructure.config.BookingProperties;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ItemDO;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.ItemMapper;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceAvailabilityResponse;
import com.laoliu.cas.appointment.domain.view.BookingQueryView;
import com.laoliu.cas.common.enums.ManageStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预约仓储实现。
 * <p>
 * 这里是「领域语义」与「SQL 字面量」的唯一翻译层：状态码统一取自 {@link ManageStatus}，
 * 幂等窗口取自 {@link BookingProperties}，Mapper 与 XML 不出现魔法数字。
 *
 * @author forever-king
 */
@Repository
@RequiredArgsConstructor
public class BookingRepositoryImpl implements BookingRepository {

    private static final int PENDING = ManageStatus.SUBMIT.getCode();
    private static final int APPROVED = ManageStatus.APPROVED.getCode();
    private static final int CANCELLED = ManageStatus.CANCELLED.getCode();
    private static final int COMPLETED = ManageStatus.COMPLETED.getCode();

    private final ItemMapper itemMapper;
    private final BookingProperties bookingProperties;

    @Override
    public List<Long> insertServices(Long userId, List<Integer> serviceIds) {
        List<Long> createdOrderIds = new ArrayList<>();
        // 逐个幂等插入：调用方已在同事务内扣减库存；此处拿到回填的真实 orderId。
        for (Integer serviceId : serviceIds) {
            ItemDO item = ItemDO.builder().userId(userId).serviceId(serviceId).build();
            int rows = itemMapper.insertSingleService(item, PENDING, APPROVED);
            if (rows > 0 && item.getOrderId() != null) {
                createdOrderIds.add(item.getOrderId().longValue());
            }
        }
        return createdOrderIds;
    }

    @Override
    public int decrementStock(Long serviceId) {
        return itemMapper.decrementStock(serviceId);
    }

    @Override
    public int releaseStock(Long serviceId) {
        return itemMapper.releaseStock(serviceId);
    }

    @Override
    public Long insertConsultationBooking(Long userId, Long serviceId, Long consultantId, Long slotId,
                                          LocalDate slotDate, String startTime, String endTime) {
        ItemDO item = ItemDO.builder()
                .userId(userId)
                .serviceId(serviceId == null ? null : serviceId.intValue())
                .consultantId(consultantId)
                .slotId(slotId)
                .slotDate(slotDate)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        int rows = itemMapper.insertConsultationBooking(item, PENDING, bookingProperties.getDedupeSeconds());
        return rows > 0 && item.getOrderId() != null ? item.getOrderId().longValue() : null;
    }

    @Override
    public int releaseSlotByOrderId(Long orderId) {
        return itemMapper.releaseSlotByOrderId(orderId);
    }

    @Override
    public int releaseStockByOrderId(Long orderId) {
        return itemMapper.releaseStockByOrderId(orderId);
    }

    @Override
    public Long insertEquipmentBooking(Long userId, Long serviceId, Long equipmentId, Integer quantity,
                                       LocalDate date, String startTime, String endTime) {
        ItemDO item = ItemDO.builder()
                .userId(userId)
                .serviceId(serviceId == null ? null : serviceId.intValue())
                .equipmentId(equipmentId)
                .quantity(quantity)
                .slotDate(date)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        int rows = itemMapper.insertEquipmentBooking(item, PENDING, bookingProperties.getDedupeSeconds());
        return rows > 0 && item.getOrderId() != null ? item.getOrderId().longValue() : null;
    }

    @Override
    public int sumEquipmentOverlap(Long equipmentId, LocalDate date, String startTime, String endTime) {
        return itemMapper.sumEquipmentOverlap(equipmentId, date, startTime, endTime, PENDING, APPROVED);
    }

    @Override
    public int autoCompleteExpired() {
        return itemMapper.autoCompleteExpired(APPROVED, COMPLETED);
    }

    @Override
    public Long insertRoomBooking(Long userId, Long serviceId, Long roomId, LocalDate date, String startTime, String endTime) {
        ItemDO item = ItemDO.builder()
                .userId(userId)
                .serviceId(serviceId == null ? null : serviceId.intValue())
                .roomId(roomId)
                .slotDate(date)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        int rows = itemMapper.insertRoomBooking(item, PENDING, bookingProperties.getDedupeSeconds());
        return rows > 0 && item.getOrderId() != null ? item.getOrderId().longValue() : null;
    }

    @Override
    public int countRoomOverlap(Long roomId, LocalDate date, String startTime, String endTime) {
        return itemMapper.countRoomOverlap(roomId, date, startTime, endTime, PENDING, APPROVED);
    }

    @Override
    public List<Long> findCancellableOrderIds(Long userId, List<Long> orderIds) {
        return itemMapper.selectCancellableOrderIds(userId, orderIds, PENDING, APPROVED);
    }

    @Override
    public int cancelByIds(Long userId, List<Long> orderIds) {
        return itemMapper.cancelByIdsAndRelease(userId, orderIds, PENDING, APPROVED, CANCELLED);
    }

    @Override
    public IPage<BookingQueryView> getServiceStatus(int page, int pageSize, Integer manageStatus, String serviceName) {
        return itemMapper.getServiceStatusWithPage(new Page<>(page, pageSize), manageStatus, serviceName);
    }

    @Override
    public List<BookingQueryView> getServiceStatusByUserId(Long userId) {
        return itemMapper.getServiceStatusByUserId(userId);
    }

    @Override
    public IPage<BookingQueryView> getServiceStatusByUserId(Long userId, int page, int pageSize, Integer manageStatus, String serviceName) {
        return itemMapper.getServiceStatusByUserIdWithPage(userId, new Page<>(page, pageSize), manageStatus, serviceName);
    }

    @Override
    public BookingQueryView getServiceStatusByOrderId(Long orderId) {
        return itemMapper.getServiceStatusByOrderId(orderId);
    }

    @Override
    public BookingQueryView getServiceStatusByOrderIdAndUserId(Long userId, Long orderId) {
        return itemMapper.getServiceStatusByOrderIdAndUserId(userId, orderId);
    }

    @Override
    public IPage<BookingQueryView> getTeacherBookings(Long teacherId, int page, int pageSize, Integer manageStatus) {
        return itemMapper.getTeacherBookingsWithPage(teacherId, new Page<>(page, pageSize), manageStatus);
    }

    @Override
    public Long selectConsultantOwnerByOrderId(Long orderId) {
        return itemMapper.selectConsultantOwnerByOrderId(orderId);
    }

    @Override
    public int approveActivityBookings(Long userId, List<Integer> serviceIds) {
        return itemMapper.approveRecentActivityBookings(
                userId, serviceIds, PENDING, APPROVED, bookingProperties.getDedupeSeconds());
    }

    @Override
    public boolean auditService(Long orderId, Integer status, String reason, List<ManageStatus> allowedFrom) {
        List<Integer> fromCodes = allowedFrom.stream()
                .map(ManageStatus::getCode)
                .collect(Collectors.toList());
        return itemMapper.auditService(orderId, status, reason, fromCodes) > 0;
    }

    @Override
    public String getUserEmailByOrderId(Long orderId) {
        return itemMapper.getUserEmailByOrderId(orderId);
    }

    @Override
    public Map<Long, Long> countBookingsByService() {
        return itemMapper.countBookingsByService(PENDING, APPROVED).stream().collect(
                Collectors.toMap(ServiceAvailabilityResponse::getServiceId,
                        vo -> vo.getBookingCount() == null ? 0L : vo.getBookingCount().longValue()));
    }
}
