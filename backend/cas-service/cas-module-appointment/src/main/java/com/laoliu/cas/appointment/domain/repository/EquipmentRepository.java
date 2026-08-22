package com.laoliu.cas.appointment.domain.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.domain.entity.Equipment;

import java.util.List;
import java.util.Optional;

/**
 * 设备领域仓库接口
 *
 * @author forever-king
 */
public interface EquipmentRepository {

    Optional<Equipment> findById(Long id);

    List<Equipment> findByServiceId(Long serviceId);

    List<Equipment> findAll();

    /** 分页查询设备，支持按名称/分类筛选 */
    IPage<Equipment> findPage(int page, int pageSize, String name, String category);

    /** 查询所有不重复的设备分类 */
    List<String> findDistinctCategories();
}
