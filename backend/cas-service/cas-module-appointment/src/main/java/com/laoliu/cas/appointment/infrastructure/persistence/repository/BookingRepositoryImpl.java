package com.laoliu.cas.appointment.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.ItemMapper;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceAvailabilityVO;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预约仓储实现
 *
 * @author forever-king
 */
@Repository
@RequiredArgsConstructor
public class BookingRepositoryImpl implements BookingRepository {

    private final ItemMapper itemMapper;

    @Override
    public int insertServices(Long userId, List<Integer> serviceIds) {
        return itemMapper.insertServices(userId, serviceIds);
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
    public List<Long> selectServiceIdsByBookingIds(Long userId, List<Long> bookingIds) {
        return itemMapper.selectServiceIdsByBookingIds(userId, bookingIds);
    }

    @Override
    public Long selectServiceIdByOrderId(Long orderId) {
        return itemMapper.selectServiceIdByOrderId(orderId);
    }

    @Override
    public int cancelBookings(Long userId, List<Long> bookingIds) {
        return itemMapper.setBookingStatusByParts(userId, bookingIds);
    }

    @Override
    public List<ServiceStatusResponse> getServiceStatus() {
        return itemMapper.getServiceStatus();
    }

    @Override
    public IPage<ServiceStatusResponse> getServiceStatus(int page, int pageSize, Integer manageStatus, String serviceName) {
        return itemMapper.getServiceStatusWithPage(new Page<>(page, pageSize), manageStatus, serviceName);
    }

    @Override
    public List<ServiceStatusResponse> getServiceStatusByUserId(Long userId) {
        return itemMapper.getServiceStatusByUserId(userId);
    }

    @Override
    public IPage<ServiceStatusResponse> getServiceStatusByUserId(Long userId, int page, int pageSize, Integer manageStatus, String serviceName) {
        return itemMapper.getServiceStatusByUserIdWithPage(userId, new Page<>(page, pageSize), manageStatus, serviceName);
    }

    @Override
    public ServiceStatusResponse getServiceStatusByOrderId(Long orderId) {
        return itemMapper.getServiceStatusByOrderId(orderId);
    }

    @Override
    public ServiceStatusResponse getServiceStatusByOrderIdAndUserId(Long userId, Long orderId) {
        return itemMapper.getServiceStatusByOrderIdAndUserId(userId, orderId);
    }

    @Override
    public boolean auditService(Long orderId, Integer status, String reason) {
        return itemMapper.auditService(orderId, status, reason) > 0;
    }

    @Override
    public String getUserEmailByOrderId(Long orderId) {
        return itemMapper.getUserEmailByOrderId(orderId);
    }

    @Override
    public Map<Long, Long> countBookingsByService() {
        return itemMapper.countBookingsByService().stream().collect(
                Collectors.toMap(ServiceAvailabilityVO::getServiceId,
                        vo -> vo.getBookingCount() == null ? 0L : vo.getBookingCount().longValue()));
    }
}
