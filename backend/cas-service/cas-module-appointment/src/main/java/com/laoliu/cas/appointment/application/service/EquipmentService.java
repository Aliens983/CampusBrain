package com.laoliu.cas.appointment.application.service;

import com.laoliu.cas.appointment.interfaces.dto.response.EquipmentResponse;

import java.util.List;

/**
 * 设备查询应用服务接口
 *
 * @author forever-king
 */
public interface EquipmentService {

    /**
     * 获取所有可借用的设备列表
     */
    List<EquipmentResponse> getAvailableEquipment();

    /**
     * 获取设备分类列表
     */
    List<String> getCategories();

    /**
     * 根据ID获取设备详情
     */
    EquipmentResponse getEquipmentById(Long id);
}
