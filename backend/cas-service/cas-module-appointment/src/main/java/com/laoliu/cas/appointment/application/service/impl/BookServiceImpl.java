package com.laoliu.cas.appointment.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.BookService;
import com.laoliu.cas.appointment.domain.entity.ServiceItem;
import com.laoliu.cas.appointment.domain.enums.CategoryCode;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.infrastructure.metrics.BookingMetrics;
import com.laoliu.cas.appointment.domain.repository.ServiceItemRepository;
import com.laoliu.cas.appointment.infrastructure.mq.BookingEventPublisher;
import com.laoliu.cas.appointment.interfaces.dto.response.BookingResponse;
import com.laoliu.cas.appointment.domain.view.BookingQueryView;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.BookErrorCode;
import com.laoliu.cas.common.exception.code.ServiceErrorCode;
import com.laoliu.cas.common.enums.ManageStatus;
import com.laoliu.cas.system.api.UserInfoApi;
import com.laoliu.cas.system.api.dto.UserInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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
    private final BookingMetrics bookingMetrics;

    public BookServiceImpl(BookingRepository bookingRepository, ServiceItemRepository serviceRepository,
                           UserInfoApi userInfoApi, BookingEventPublisher bookingEventPublisher,
                           BookingMetrics bookingMetrics) {
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
        this.userInfoApi = userInfoApi;
        this.bookingEventPublisher = bookingEventPublisher;
        this.bookingMetrics = bookingMetrics;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // booked_count 变了，services 缓存里的余量/容量快照立即失效。
    // 否则助手侧最长 30 分钟内会拿过期快照判断"是否还有名额"，
    // 出现"显示可约、确认却被拒"或"显示已满、实际可约"。
    @CacheEvict(value = "services", allEntries = true)
    public BookingSubmitResult bookService(Long userId, List<Long> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            throw new BusinessException(ServiceErrorCode.SERVICE_ID_EMPTY);
        }

        List<Integer> activityServiceIds = new ArrayList<>();
        List<Long> createdOrderIds = new ArrayList<>();
        try {
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
                // 乐观锁扣减库存（同事务）：容量充足才 +1，满则抛异常，事务回滚
                if (bookingRepository.decrementStock(sid) == 0) {
                    bookingMetrics.recordConflictBlocked(BookingMetrics.REASON_CAPACITY_FULL);
                    throw new BusinessException(BookErrorCode.BOOKING_CAPACITY_FULL, sid);
                }
                List<Long> created = bookingRepository.insertServices(userId, List.of(sid.intValue()));
                if (created.isEmpty()) {
                    // 重复预约被 SQL 幂等去重拦下：必须把刚才扣掉的名额还回去。
                    // 此前批量下单对"被去重的项"不做回补，booked_count 凭空 +1 却没有对应订单，
                    // 名额只能靠后续某次取消的 GREATEST(x-1,0) 偶然抵消。
                    bookingRepository.releaseStock(sid);
                    log.info("预约去重：同用户同服务已存在生效预约，已回补库存: userId={}, serviceId={}",
                            userId, sid);
                    continue;
                }
                createdOrderIds.addAll(created);
                // 活动预约：容量够即直通，不走人工审核（categoryId → service_category.code）
                if (CategoryCode.ACTIVITY.is(service.getCategoryCode())) {
                    activityServiceIds.add(sid.intValue());
                }
                bookingEventPublisher.publishChanged(userId, sid, "BOOKED");
            }
            // 一个都没建成说明整单都是重复提交，回滚（此处已无库存可回滚，仅为语义明确的报错）
            if (createdOrderIds.isEmpty()) {
                throw new BusinessException(BookErrorCode.BOOKING_REPEATED);
            }
            // 免审直通：刚落库的活动预约置为「已通过」，不产生待审核
            if (!activityServiceIds.isEmpty()) {
                bookingRepository.approveActivityBookings(userId, activityServiceIds);
            }
            bookingMetrics.recordCreated(createdOrderIds.size());
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
        IPage<BookingQueryView> statusPage = bookingRepository.getServiceStatusByUserId(
                userId, page, pageSize, null, null);
        return statusPage.convert(this::convertToDTO);
    }

    private BookingResponse convertToDTO(BookingQueryView status) {
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
    // 取消会回补 booked_count，同 bookService 需要让余量快照立即失效
    @CacheEvict(value = "services", allEntries = true)
    public boolean cancelBookings(Long userId, List<Long> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) {
            return false;
        }
        // 先查出真正满足取消条件（本人所有 + 待审核/已通过活动单）的订单（12-09）。
        // 不能拿入参集合直接发事件：不属于本人、已取消、已通过的非活动单都不会被 UPDATE，
        // 给它们发 CANCELLED 会让 KB 侧按未发生的变更淘汰缓存，属于虚假业务事件。
        List<Long> cancellableIds = bookingRepository.findCancellableOrderIds(userId, bookingIds);
        if (cancellableIds.isEmpty()) {
            return false;
        }
        // 按实际命中集合执行原子多表 UPDATE；SELECT 与 UPDATE 间状态被并发改动时，
        // 以 UPDATE 影响行数为准（极端竞争下 affected 可能小于 cancellableIds）
        int affected = bookingRepository.cancelByIds(userId, cancellableIds);
        if (affected <= 0) {
            return false;
        }
        bookingMetrics.recordCancelled(affected);
        // 库存回补与咨询时段释放已在一条多表 UPDATE 内原子完成：
        // 只释放本次真正取消的通用单/咨询单，资源单不误扣、历史单不重复释放（7.3.6）。
        // 事件同样只发给实际命中的订单（12-09）。
        for (Long id : cancellableIds) {
            bookingEventPublisher.publishChanged(userId, id, "CANCELLED");
        }
        return true;
    }

    @Override
    public BookingResponse getBookingById(Long userId, Long orderId) {
        BookingQueryView status = bookingRepository.getServiceStatusByOrderIdAndUserId(userId, orderId);
        if (status == null) {
            return null;
        }
        return convertToDTO(status);
    }
}
