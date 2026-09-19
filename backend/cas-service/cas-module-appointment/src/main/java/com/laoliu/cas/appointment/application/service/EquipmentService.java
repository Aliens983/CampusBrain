package com.laoliu.cas.appointment.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.dto.request.EquipmentBookRequest;
import com.laoliu.cas.appointment.application.dto.response.EquipmentResponse;

import java.util.List;

/**
 * 设备应用服务接口（设备查询与设备借用）
 *
 * @author forever-king
 */
public interface EquipmentService {

    /**
     * 获取所有可借用的设备列表
     */
    List<EquipmentResponse> getAvailableEquipment();

    /**
     * 分页获取可借用设备，支持按名称/分类/所属服务筛选
     */
    IPage<EquipmentResponse> getAvailableEquipment(int page, int pageSize, String name, String category, Long serviceId);

    /**
     * 获取设备分类列表
     */
    List<String> getCategories();

    /**
     * 根据ID获取设备详情
     */
    EquipmentResponse getEquipmentById(Long id);

    /**
     * 为指定用户借用设备（按单日窗口动态校验库存）
     *
     * @param userId      借用用户 ID
     * @param equipmentId 设备 ID
     * @param req         数量 + 日期 + 起止时间（HH:mm，单日窗口）
     * @return 新预约单 orderId
     */
    Long bookEquipment(Long userId, Long equipmentId, EquipmentBookRequest req);
}
