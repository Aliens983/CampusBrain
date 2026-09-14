package com.laoliu.cas.appointment.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.ConsultationService;
import com.laoliu.cas.appointment.domain.entity.Consultant;
import com.laoliu.cas.appointment.domain.entity.ServiceItem;
import org.springframework.stereotype.Service;
import com.laoliu.cas.appointment.domain.entity.TimeSlot;
import com.laoliu.cas.appointment.domain.enums.CategoryCode;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.domain.repository.ConsultantRepository;
import com.laoliu.cas.appointment.domain.repository.ServiceItemRepository;
import com.laoliu.cas.appointment.domain.repository.TimeSlotRepository;
import com.laoliu.cas.appointment.infrastructure.mq.BookingEventPublisher;
import com.laoliu.cas.appointment.infrastructure.metrics.BookingMetrics;
import com.laoliu.cas.appointment.interfaces.dto.response.ConsultantResponse;
import com.laoliu.cas.appointment.interfaces.dto.response.TimeSlotResponse;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.BookErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 咨询查询/咨询时段预约应用服务实现 — 咨询师、时段均从数据库读取真实数据
 *
 * @author forever-king
 */
@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final ServiceItemRepository serviceRepository;
    private final ConsultantRepository consultantRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final BookingRepository bookingRepository;
    private final BookingEventPublisher bookingEventPublisher;
    private final BookingMetrics bookingMetrics;

    @Override
    public List<ConsultantResponse> getAvailableConsultants() {
        List<Long> consultationServiceIds = getConsultationServiceIds();
        if (consultationServiceIds.isEmpty()) {
            return Collections.emptyList();
        }
        return consultationServiceIds.stream()
                .flatMap(serviceId -> consultantRepository.findByServiceId(serviceId).stream())
                .map(this::toConsultantResponse)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询咨询师，支持按名称/部门/所属服务筛选
     */
    @Override
    public IPage<ConsultantResponse> getAvailableConsultants(int page, int pageSize, String name, String department, Long serviceId) {
        IPage<Consultant> consultantPage = consultantRepository.findPage(page, pageSize, name, department, serviceId);
        return consultantPage.convert(this::toConsultantResponse);
    }

    @Override
    public ConsultantResponse getConsultantById(Long id) {
        return consultantRepository.findById(id)
                .map(this::toConsultantResponse)
                .orElse(null);
    }

    @Override
    public List<TimeSlotResponse> getAvailableTimeSlots(Long consultantId, String date) {
        return consultantRepository.findTimeSlots(consultantId, date);
    }

    /**
     * 咨询时段预约：用户选定某咨询师的某个时段后，
     * 事务内原子占用时段 + 幂等落单；任一失败整体回滚（时段自动释放）。
     * <p>
     * 并发安全：占用使用条件 UPDATE（available=1 → 0），同一时段只会有一个请求成功，
     * 其余拿到 {@code SLOT_UNAVAILABLE}。
     *
     * @param slotId 由 /slots 接口返回的时段 ID
     * @return 新预约单 orderId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long bookConsultation(Long userId, Long consultantId, Long slotId) {
        if (consultantId == null || slotId == null) {
            throw new BusinessException(BookErrorCode.BOOKING_FAILED);
        }
        Consultant consultant = consultantRepository.findById(consultantId)
                .orElseThrow(() -> new BusinessException(BookErrorCode.CONSULTANT_NOT_FOUND));

        TimeSlot slot = timeSlotRepository.findById(slotId)
                .filter(s -> Objects.equals(s.getConsultantId(), consultantId))
                .orElseThrow(() -> new BusinessException(BookErrorCode.SLOT_MISMATCH));
        if (!slot.isAvailable()) {
            bookingMetrics.recordConflictBlocked(BookingMetrics.REASON_SLOT_UNAVAILABLE);
            throw new BusinessException(BookErrorCode.SLOT_UNAVAILABLE);
        }

        // 原子占用：仅 available=1 时置 0；失败=刚被抢走
        if (!timeSlotRepository.occupy(slotId)) {
            bookingMetrics.recordConflictBlocked(BookingMetrics.REASON_SLOT_UNAVAILABLE);
            throw new BusinessException(BookErrorCode.SLOT_UNAVAILABLE);
        }

        // 幂等插入（同咨询师同时段配置窗口内同用户去重）；null → 重复提交，回滚释放时段。
        // orderId 由 insert 回填，直接返回真实订单号（助手确认预约不再靠"查最新一单"猜测）
        Long orderId = bookingRepository.insertConsultationBooking(
                userId, consultant.getServiceId(), consultantId, slotId,
                slot.getSlotDate(), slot.getStartTime(), slot.getEndTime());
        if (orderId == null) {
            throw new BusinessException(BookErrorCode.BOOKING_REPEATED);
        }

        bookingEventPublisher.publishChanged(userId, consultant.getServiceId(), "BOOKED");
        return orderId;
    }

    private List<Long> getConsultationServiceIds() {
        return serviceRepository.findAll().stream()
                .filter(ServiceItem::isAvailable)
                // 3.1.6：按业务分类编码判定，而非在服务名里匹配"咨询/辅导"等关键词
                .filter(s -> CategoryCode.TEACHER.is(s.getCategoryCode()))
                .map(ServiceItem::getServiceId)
                .collect(Collectors.toList());
    }

    private ConsultantResponse toConsultantResponse(Consultant consultant) {
        return ConsultantResponse.builder()
                .id(consultant.getId())
                .name(consultant.getName())
                .title(consultant.getTitle())
                .department(consultant.getDepartment())
                .expertise(consultant.getDescription() != null
                        ? Collections.singletonList(consultant.getDescription()) : Collections.emptyList())
                .rating(consultant.getRating() != null ? consultant.getRating().doubleValue() : null)
                .reviews(consultant.getReviewCount() != null ? consultant.getReviewCount() : 0)
                .available(consultant.hasRatings())
                .avatar(consultant.getAvatarUrl() != null ? consultant.getAvatarUrl() : "")
                .build();
    }
}
