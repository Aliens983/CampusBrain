package com.laoliu.cas.appointment.domain.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.domain.entity.ServiceItem;
import java.util.List;
import java.util.Optional;

/**
 * 服务领域仓库接口
 * 定义领域行为，不关心实现细节
 *
 * @author forever-king
 */
public interface ServiceItemRepository {

    /**
     * 根据 ID 查找服务
     */
    Optional<ServiceItem> findById(Long id);

    /**
     * 查找所有服务
     */
    List<ServiceItem> findAll();

    /**
     * 分页查找所有服务（支持按名称模糊搜索、按状态、按校区筛选）
     */
    IPage<ServiceItem> findAll(int page, int pageSize, String serviceName, Integer serviceState, String campus);

    /**
     * 根据创建者 ID 查找服务
     */
    List<ServiceItem> findByUserId(Long userId);

    /**
     * 分页查找用户的服务
     */
    IPage<ServiceItem> findByUserId(Long userId, int page, int pageSize);

    /**
     * 保存服务（新增或更新）
     */
    ServiceItem save(ServiceItem service);

    /**
     * 根据 ID 删除服务
     */
    void deleteById(Long id);

    /**
     * 检查服务是否存在
     */
    boolean existsById(Long id);
}
