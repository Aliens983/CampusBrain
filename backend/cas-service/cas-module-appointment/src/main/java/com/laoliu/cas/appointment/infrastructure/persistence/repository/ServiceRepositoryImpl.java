package com.laoliu.cas.appointment.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laoliu.cas.appointment.domain.entity.Service;
import com.laoliu.cas.appointment.domain.entity.ServiceCategory;
import com.laoliu.cas.appointment.domain.repository.ServiceCategoryRepository;
import com.laoliu.cas.appointment.domain.repository.ServiceRepository;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ServicesDO;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.ServiceMapper;
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
public class ServiceRepositoryImpl implements ServiceRepository {

    private final ServiceMapper serviceMapper;
    private final ServiceCategoryRepository serviceCategoryRepository;

    @Override
    public Optional<Service> findById(Long id) {
        ServicesDO dataObject = serviceMapper.selectById(id);
        return Optional.ofNullable(dataObject).map(ServicesDO::toEntity).map(this::enrich);
    }

    @Override
    public List<Service> findAll() {
        return serviceMapper.selectList(null).stream()
                .map(ServicesDO::toEntity)
                .map(this::enrich)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<Service> findAll(int page, int pageSize, String serviceName, Integer serviceState) {
        LambdaQueryWrapper<ServicesDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(serviceName)) {
            wrapper.like(ServicesDO::getServiceName, serviceName);
        }
        if (serviceState != null) {
            wrapper.eq(ServicesDO::getServiceState, serviceState);
        }
        wrapper.orderByDesc(ServicesDO::getServiceId);

        Page<ServicesDO> pageParam = new Page<>(page, pageSize);
        IPage<ServicesDO> doPage = serviceMapper.selectPage(pageParam, wrapper);
        return doPage.convert(row -> enrich(row.toEntity()));
    }

    @Override
    public List<Service> findByUserId(Long userId) {
        return serviceMapper.selectUserServices(userId).stream()
                .map(ServicesDO::toEntity)
                .map(this::enrich)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<Service> findByUserId(Long userId, int page, int pageSize) {
        Page<ServicesDO> pageParam = new Page<>(page, pageSize);
        IPage<ServicesDO> doPage = serviceMapper.selectUserServicesWithPage(userId, pageParam);
        return doPage.convert(row -> enrich(row.toEntity()));
    }

    @Override
    public Service save(Service service) {
        ServicesDO dataObject = ServicesDO.fromEntity(service);
        if (service.getServiceId() == null) {
            serviceMapper.insert(dataObject);
        } else {
            serviceMapper.updateById(dataObject);
        }
        return enrich(dataObject.toEntity());
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
    private Service enrich(Service service) {
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
