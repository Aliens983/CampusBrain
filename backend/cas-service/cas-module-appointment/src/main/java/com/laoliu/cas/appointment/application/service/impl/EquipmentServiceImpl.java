package com.laoliu.cas.appointment.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.EquipmentService;
import com.laoliu.cas.appointment.domain.entity.Equipment;
import com.laoliu.cas.appointment.domain.entity.Service;
import com.laoliu.cas.appointment.domain.repository.EquipmentRepository;
import com.laoliu.cas.appointment.domain.repository.ServiceRepository;
import com.laoliu.cas.appointment.interfaces.dto.response.EquipmentResponse;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备查询应用服务实现 — 全部从数据库读取真实数据
 *
 * @author forever-king
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {

    private final ServiceRepository serviceRepository;
    private final EquipmentRepository equipmentRepository;

    @Override
    public List<EquipmentResponse> getAvailableEquipment() {
        return getEquipmentServiceIds().stream()
                .flatMap(serviceId -> equipmentRepository.findByServiceId(serviceId).stream())
                .map(this::toEquipmentResponse)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询设备，支持按名称/分类筛选
     */
    public IPage<EquipmentResponse> getAvailableEquipment(int page, int pageSize, String name, String category) {
        IPage<Equipment> equipmentPage = equipmentRepository.findPage(page, pageSize, name, category);
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
