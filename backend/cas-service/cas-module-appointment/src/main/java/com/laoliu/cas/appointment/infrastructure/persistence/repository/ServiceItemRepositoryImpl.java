package com.laoliu.cas.appointment.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laoliu.cas.appointment.domain.entity.ServiceItem;
import com.laoliu.cas.appointment.domain.entity.ServiceCategory;
import com.laoliu.cas.appointment.domain.repository.ServiceCategoryRepository;
import com.laoliu.cas.appointment.domain.repository.ServiceItemRepository;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ServiceItemDO;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.ServiceItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 服务仓库实现 - 基础设施层
 *
 * @author forever-king
 */
@Repository
@RequiredArgsConstructor
public class ServiceItemRepositoryImpl implements ServiceItemRepository {

    private final ServiceItemMapper serviceMapper;
    private final ServiceCategoryRepository serviceCategoryRepository;

    @Override
    public Optional<ServiceItem> findById(Long id) {
        ServiceItemDO dataObject = serviceMapper.selectById(id);
        return Optional.ofNullable(dataObject).map(ServiceItemDO::toEntity).map(this::enrich);
    }

    @Override
    public List<ServiceItem> findAll() {
        return serviceMapper.selectList(null).stream()
                .map(ServiceItemDO::toEntity)
                .map(this::enrich)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<ServiceItem> findAll(int page, int pageSize, String serviceName, Integer serviceState, String campus) {
        LambdaQueryWrapper<ServiceItemDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(serviceName)) {
            wrapper.like(ServiceItemDO::getServiceName, serviceName);
        }
        if (serviceState != null) {
            wrapper.eq(ServiceItemDO::getServiceState, serviceState);
        }
        if (StringUtils.hasText(campus)) {
            wrapper.eq(ServiceItemDO::getCampus, campus);
        }
        wrapper.orderByDesc(ServiceItemDO::getServiceId);

        Page<ServiceItemDO> pageParam = new Page<>(page, pageSize);
        IPage<ServiceItemDO> doPage = serviceMapper.selectPage(pageParam, wrapper);
        return doPage.convert(row -> enrich(row.toEntity()));
    }

    @Override
    public List<ServiceItem> findByUserId(Long userId) {
        return serviceMapper.selectUserServices(userId).stream()
                .map(ServiceItemDO::toEntity)
                .map(this::enrich)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<ServiceItem> findByUserId(Long userId, int page, int pageSize) {
        Page<ServiceItemDO> pageParam = new Page<>(page, pageSize);
        IPage<ServiceItemDO> doPage = serviceMapper.selectUserServicesWithPage(userId, pageParam);
        return doPage.convert(row -> enrich(row.toEntity()));
    }

    @Override
    public ServiceItem save(ServiceItem service) {
        if (service.getServiceId() == null) {
            ServiceItemDO dataObject = ServiceItemDO.fromEntity(service);
            serviceMapper.insert(dataObject);
            return enrich(dataObject.toEntity());
        }
        serviceMapper.updateByPrimaryKeySelective(toProfileUpdateDO(service));
        return enrich(service);
    }

    /**
     * 管理端服务更新的写入对象：只允许携带可编辑列，显式剔除 booked_count.
     *
     * <p>booked_count 由下单/取消的条件更新原子维护（decrementStock/releaseStock）。
     * 若随"加载-修改-保存"的全量实体回写，读取之后发生的并发下单/取消会被陈旧值
     * 静默覆盖（丢失更新）：名额扣减被抹掉可导致活动服务超卖，回补被抹掉则服务被
     * "幽灵占满"永久约不上，且无对账机制自愈。配合 Mapper 层不含该列的选择性更新，
     * 从两个层面保证 booked_count 不会被应用层全量回写。
     */
    private ServiceItemDO toProfileUpdateDO(ServiceItem service) {
        ServiceItemDO dataObject = ServiceItemDO.fromEntity(service);
        dataObject.setBookedCount(null);
        return dataObject;
    }

    @Override
    public void deleteById(Long id) {
        serviceMapper.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return serviceMapper.selectById(id) != null;
    }

    /** 按 categoryId 回填分类编码与中文名（代码级外键解析，分类表固定 4 行） */
    private ServiceItem enrich(ServiceItem service) {
        if (service == null || service.getCategoryId() == null) {
            return service;
        }
        ServiceCategory category = categoryIndex().get(service.getCategoryId());
        if (category != null) {
            service.setCategoryCode(category.getCode());
            service.setCategoryName(category.getName());
        }
        return service;
    }

    private Map<Long, ServiceCategory> categoryIndex() {
        return serviceCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(ServiceCategory::getId, Function.identity()));
    }
}
