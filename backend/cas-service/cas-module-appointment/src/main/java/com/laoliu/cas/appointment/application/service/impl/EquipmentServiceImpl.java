package com.laoliu.cas.appointment.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.EquipmentService;
import com.laoliu.cas.appointment.domain.entity.Equipment;
import com.laoliu.cas.appointment.domain.entity.Service;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.domain.repository.EquipmentRepository;
import com.laoliu.cas.appointment.domain.repository.ServiceRepository;
import com.laoliu.cas.appointment.infrastructure.mq.BookingEventPublisher;
import com.laoliu.cas.appointment.interfaces.dto.request.EquipmentBookRequest;
import com.laoliu.cas.appointment.interfaces.dto.response.EquipmentResponse;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.BookErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备查询/设备借用应用服务实现 — 全部从数据库读取真实数据
 *
 * @author forever-king
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {

    private final ServiceRepository serviceRepository;
    private final EquipmentRepository equipmentRepository;
    private final BookingRepository bookingRepository;
    private final BookingEventPublisher bookingEventPublisher;

    @Override
    public List<EquipmentResponse> getAvailableEquipment() {
        return getEquipmentServiceIds().stream()
                .flatMap(serviceId -> equipmentRepository.findByServiceId(serviceId).stream())
                .map(this::toEquipmentResponse)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询设备，支持按名称/分类/所属服务筛选
     */
    public IPage<EquipmentResponse> getAvailableEquipment(int page, int pageSize, String name, String category, Long serviceId) {
        IPage<Equipment> equipmentPage = equipmentRepository.findPage(page, pageSize, name, category, serviceId);
        return equipmentPage.convert(this::toEquipmentResponse);
    }

    @Override
    public List<String> getCategories() {
        return equipmentRepository.findDistinctCategories();
    }

    @Override
    public EquipmentResponse getEquipmentById(Long id) {
        return equipmentRepository.findById(id)
                .map(this::toEquipmentResponse)
                .orElse(null);
    }

    /**
     * 设备借用（固定时间段窗口，到点由定时任务自动归还）：
     * <p>
     * 并发安全：事务内先对设备行加锁（SELECT ... FOR UPDATE），
     * 再校验"该设备该日该时段已占用台数 + 本次数量 ≤ available_stock"，
     * 满足才幂等落单；任何校验失败整体回滚，不会超借。
     *
     * @param equipmentId 设备ID
     * @param req         数量 + 日期 + 起止时间（HH:mm，单日窗口）
     */
    @Transactional(rollbackFor = Exception.class)
    public void bookEquipment(Long userId, Long equipmentId, EquipmentBookRequest req) {
        if (equipmentId == null || req == null || req.getQuantity() == null || req.getQuantity() < 1) {
            throw new BusinessException(BookErrorCode.BORROW_TIME_INVALID);
        }
        if (req.getStartTime() == null || req.getEndTime() == null
                || req.getStartTime().compareTo(req.getEndTime()) >= 0) {
            throw new BusinessException(BookErrorCode.BORROW_TIME_INVALID);
        }
        LocalDate date;
        try {
            date = LocalDate.parse(req.getDate());
        } catch (Exception e) {
            throw new BusinessException(BookErrorCode.BORROW_TIME_INVALID);
        }
        if (date.isBefore(LocalDate.now())) {
            throw new BusinessException(BookErrorCode.BORROW_TIME_INVALID);
        }

        // 行锁读取设备，串行化同一设备的借用判定，防并发超借
        Equipment equipment = equipmentRepository.findByIdForUpdate(equipmentId)
                .orElseThrow(() -> new BusinessException(BookErrorCode.EQUIPMENT_NOT_FOUND));
        int stock = equipment.getAvailableStock() == null ? 0 : equipment.getAvailableStock();
        int occupied = bookingRepository.sumEquipmentOverlap(
                equipmentId, date, req.getStartTime(), req.getEndTime());
        if (occupied + req.getQuantity() > stock) {
            throw new BusinessException(BookErrorCode.EQUIPMENT_STOCK_NOT_ENOUGH);
        }

        int inserted = bookingRepository.insertEquipmentBooking(
                userId, equipment.getServiceId(), equipmentId, req.getQuantity(),
                date, req.getStartTime(), req.getEndTime());
        if (inserted == 0) {
            throw new BusinessException(BookErrorCode.BOOKING_REPEATED);
        }
        bookingEventPublisher.publishChanged(userId, equipment.getServiceId(), "BOOKED");
    }

    private List<Long> getEquipmentServiceIds() {
        return serviceRepository.findAll().stream()
                .filter(Service::isAvailable)
                .map(Service::getServiceId)
                .collect(Collectors.toList());
    }

    private EquipmentResponse toEquipmentResponse(Equipment equipment) {
        return EquipmentResponse.builder()
                .id(equipment.getId())
                .name(equipment.getName())
                .category(equipment.getCategory() != null ? equipment.getCategory() : "其他设备")
                .description(equipment.getDescription())
                .stock(equipment.getTotalStock())
                .availableStock(equipment.getAvailableStock())
                .unit(equipment.getUnit())
                .priceLabel(equipment.isAvailable() ? "可借用" : "暂不可借")
                .location(equipment.getLocation())
                .image(equipment.getImageUrl() != null ? equipment.getImageUrl() : "")
                .build();
    }
}
