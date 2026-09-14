package com.laoliu.cas.appointment.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.BookService;
import com.laoliu.cas.appointment.domain.entity.ServiceItem;
import com.laoliu.cas.appointment.domain.enums.CategoryCode;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.domain.repository.ServiceItemRepository;
import com.laoliu.cas.appointment.infrastructure.mq.BookingEventPublisher;
import com.laoliu.cas.appointment.interfaces.dto.response.BookingResponse;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.BookErrorCode;
import com.laoliu.cas.common.exception.code.ServiceErrorCode;
import com.laoliu.cas.common.enums.ManageStatus;
import com.laoliu.cas.system.api.UserInfoApi;
import com.laoliu.cas.system.api.dto.UserInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 预约业务应用服务实现
 *
 * @author forever-king
 */
@Slf4j
@Service
public class BookServiceImpl implements BookService {

    private final BookingRepository bookingRepository;
    private final ServiceItemRepository serviceRepository;
    private final UserInfoApi userInfoApi;
    private final BookingEventPublisher bookingEventPublisher;

    public BookServiceImpl(BookingRepository bookingRepository, ServiceItemRepository serviceRepository, UserInfoApi userInfoApi, BookingEventPublisher bookingEventPublisher) {
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
        this.userInfoApi = userInfoApi;
        this.bookingEventPublisher = bookingEventPublisher;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookingSubmitResult bookService(Long userId, List<Long> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            throw new BusinessException(ServiceErrorCode.SERVICE_ID_EMPTY);
        }

        List<Integer> activityServiceIds = new ArrayList<>();
        for (Long sid : serviceIds) {
            // 防止 Long→Integer 转换溢出（service_id 为 INT）
            if (sid == null || sid > Integer.MAX_VALUE) {
                throw new BusinessException(BookErrorCode.BOOKING_FAILED);
            }
            ServiceItem service = serviceRepository.findById(sid)
                    .orElseThrow(() -> new BusinessException(ServiceErrorCode.SERVICE_NOT_EXIST, sid));
            if (!service.isAvailable()) {
                throw new BusinessException(ServiceErrorCode.SERVICE_DISABLED, sid);
            }
            // 设备借用必须走专用端点 /app/equipment/{equipmentId}/book。
            // 通用下单拿不到 equipmentId/时段/数量：既无法参与设备时段占用校验（会超借），
            // 又会让 services.booked_count 与 equipment.available_stock 两套口径分裂。
            if (CategoryCode.EQUIPMENT.is(service.getCategoryCode())) {
                throw new BusinessException(BookErrorCode.EQUIPMENT_REQUIRE_DEDICATED_API, sid);
            }
            // 活动预约：容量够即直通，不走人工审核（categoryId → service_category.code）
            if (CategoryCode.ACTIVITY.is(service.getCategoryCode())) {
                activityServiceIds.add(sid.intValue());
            }
            // 乐观锁扣减库存（同事务）：容量充足才 +1，满则抛异常，事务回滚
            if (bookingRepository.decrementStock(sid) == 0) {
                throw new BusinessException(BookErrorCode.BOOKING_CAPACITY_FULL, sid);
            }
        }

        try {
            List<Integer> serviceIdInts = serviceIds.stream()
                    .map(Long::intValue)
                    .collect(Collectors.toList());
            // 幂等插入：配置窗口内同用户同服务（待审核/已通过）会被 SQL 去重；
            // 返回本次真实新建的订单号；一个都没建成说明整单都是重复提交，回滚（含库存扣减）
            List<Long> createdOrderIds = bookingRepository.insertServices(userId, serviceIdInts);
            if (createdOrderIds.isEmpty()) {
                throw new BusinessException(BookErrorCode.BOOKING_REPEATED);
            }
            // 免审直通：刚落库的活动预约置为「已通过」，不产生待审核
            if (!activityServiceIds.isEmpty()) {
                bookingRepository.approveActivityBookings(userId, activityServiceIds);
            }
            for (Long sid : serviceIds) {
                bookingEventPublisher.publishChanged(userId, sid, "BOOKED");
            }
            return new BookingSubmitResult(userInfoApi.getUserById(userId), createdOrderIds);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("预约失败: userId={}, serviceIds={}", userId, serviceIds, e);
            throw new BusinessException(BookErrorCode.BOOKING_FAILED);
        }
    }

    @Override
    public List<BookingResponse> getAllBookings(Long userId) {
        return bookingRepository.getServiceStatusByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<BookingResponse> getAllBookings(Long userId, int page, int pageSize) {
        IPage<ServiceStatusResponse> statusPage = bookingRepository.getServiceStatusByUserId(
                userId, page, pageSize, null, null);
        return statusPage.convert(this::convertToDTO);
    }

    private BookingResponse convertToDTO(ServiceStatusResponse status) {
        BookingResponse dto = new BookingResponse();
        dto.setOrderId(status.getOrderId());
        dto.setUserId(status.getUserId());
        dto.setServiceName(status.getServiceName());
        dto.setCampus(status.getCampus());
        dto.setStatus(status.getManageStatus());
        dto.setCreateTime(status.getCreateTime());
        dto.setReason(status.getStatusDescription());
        dto.setStatusDescription(getStatusDescription(status.getManageStatus()));
        // 咨询时段预约：回显咨询师与时段
        dto.setConsultantName(status.getConsultantName());
        dto.setSlotDate(status.getSlotDate());
        dto.setStartTime(status.getStartTime());
        dto.setEndTime(status.getEndTime());
        // 设备借用：回显设备与数量
        dto.setEquipmentName(status.getEquipmentName());
        dto.setQuantity(status.getQuantity());
        // 教室预约：回显教室名
        dto.setRoomName(status.getRoomName());
        return dto;
    }

    /**
     * 状态中文描述统一取自 {@link ManageStatus} 枚举（{@link ManageStatus#of(Integer)}）。
     * <p>
     * 此前这里与 {@code ServiceStatusServiceImpl#setStatusDescription} 各写一份 switch，
     * 新增状态或改文案时极易只改一处，造成同一状态在列表页与详情页显示不一致。
     */
    private String getStatusDescription(Integer status) {
        ManageStatus manageStatus = ManageStatus.of(status);
        return manageStatus == null ? "未知状态" : manageStatus.getMessage();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelBookings(Long userId, List<Long> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) {
            return false;
        }
        boolean success = bookingRepository.cancelBookings(userId, bookingIds) > 0;
        if (success) {
            // 取消成功：释放这些预约占用的库存（仅当前用户待审核的单，防他人/重复释放）
            List<Long> serviceIds = bookingRepository.selectServiceIdsByBookingIds(userId, bookingIds);
            if (serviceIds != null) {
                serviceIds.forEach(bookingRepository::releaseStock);
            }
            // 咨询时段预约：同时释放占用的老师时段
            bookingRepository.releaseSlotsByBookingIds(userId, bookingIds);
            for (Long id : bookingIds) {
                bookingEventPublisher.publishChanged(userId, id, "CANCELLED");
            }
        }
        return success;
    }

    @Override
    public BookingResponse getBookingById(Long userId, Long orderId) {
        ServiceStatusResponse status = bookingRepository.getServiceStatusByOrderIdAndUserId(userId, orderId);
        if (status == null) {
            return null;
        }
        return convertToDTO(status);
    }
}
