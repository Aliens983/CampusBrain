package com.laoliu.cas.appointment.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.ServiceItemService;
import com.laoliu.cas.appointment.domain.entity.ServiceItem;
import org.springframework.stereotype.Service;
import com.laoliu.cas.appointment.domain.entity.ServiceCategory;
import com.laoliu.cas.appointment.domain.repository.ServiceCategoryRepository;
import com.laoliu.cas.appointment.domain.repository.ServiceItemRepository;
import com.laoliu.cas.appointment.interfaces.dto.request.ServiceAddRequest;
import com.laoliu.cas.appointment.interfaces.dto.request.ServicePageRequest;
import com.laoliu.cas.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 服务管理应用服务实现
 *
 * @author forever-king
 */
@Service
@RequiredArgsConstructor
public class ServiceItemServiceImpl implements ServiceItemService {

    private final ServiceItemRepository serviceRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;

    @Override
    public List<ServiceItem> getAllServices() {
        return serviceRepository.findAll();
    }

    @Override
    public PageResult<ServiceItem> getAllServices(ServicePageRequest req) {
        IPage<ServiceItem> result = serviceRepository.findAll(
                req.getPageNo(), req.getPageSize(),
                req.getServiceName(), req.getServiceState(), req.getCampus());
        return PageResult.of(result);
    }

    @Override
    @Cacheable(value = "services", key = "'available'")
    public List<ServiceItem> getAvailableServices() {
        // 必须返回可变的 ArrayList：JDK16 Stream.toList() 是不可变(final) List，
        // 在 NON_FINAL 类型策略下顶层不带类型包装 → 缓存二次读 SerializationException(2026-09-08)。
        return new ArrayList<>(serviceRepository.findAll().stream()
                .filter(ServiceItem::isAvailable)
                .toList());
    }

    @Override
    @Cacheable(value = "services", key = "#id")
    public Optional<ServiceItem> getServiceById(Long id) {
        return serviceRepository.findById(id);
    }

    @Override
    @CacheEvict(value = "services", allEntries = true)
    public boolean addService(ServiceAddRequest request) {
        // 代码级外键：新增服务必须指定存在的分类（service_category 固定 4 类，不允许库外 id）
        ServiceCategory category = request.getCategoryId() == null ? null
                : serviceCategoryRepository.findAll().stream()
                        .filter(c -> c.getId().equals(request.getCategoryId()))
                        .findFirst()
                        .orElse(null);
        if (category == null) {
            return false;
        }
        ServiceItem service = ServiceItem.builder()
                .serviceName(request.getServiceName())
                .serviceDescribe(request.getServiceDescribe())
                .serviceState(request.getServiceState() == null ? 1 : request.getServiceState())
                .categoryId(category.getId())
                .campus(normalizeCampus(request.getCampus()))
                .capacity(request.getCapacity() == null ? -1 : request.getCapacity())
                .imageUrl(request.getImageUrl())
                // 通用/活动类服务的可预约截止日：定时任务据此把过期预约置为「已完成」。
                // V6 已建列但此前无写入点，导致 end_date 恒为 NULL，无时段的预约永不完结
                .endDate(request.getEndDate())
                .build();
        serviceRepository.save(service);
        return true;
    }

    @Override
    @CacheEvict(value = "services", allEntries = true)
    public boolean updateService(Long id, ServiceAddRequest request) {
        ServiceItem existing = serviceRepository.findById(id).orElse(null);
        if (existing == null) return false;
        existing.setServiceName(request.getServiceName());
        existing.setServiceDescribe(request.getServiceDescribe());
        if (request.getServiceState() != null) existing.setServiceState(request.getServiceState());
        // 分类、校区创建后不可改（避免资源归属错位）；容量、封面可更新
        if (request.getCapacity() != null) existing.setCapacity(request.getCapacity());
        if (request.getImageUrl() != null) existing.setImageUrl(request.getImageUrl());
        if (request.getEndDate() != null) existing.setEndDate(request.getEndDate());
        serviceRepository.save(existing);
        return true;
    }

    /** 校区仅支持 cq/xs，非法回退 cq */
    private String normalizeCampus(String campus) {
        return "xs".equals(campus) ? "xs" : "cq";
    }

    @Override
    public List<ServiceItem> selectUserServices(Long userId) {
        return serviceRepository.findByUserId(userId);
    }

    @Override
    public PageResult<ServiceItem> selectUserServices(Long userId, int page, int pageSize) {
        IPage<ServiceItem> result = serviceRepository.findByUserId(userId, page, pageSize);
        return PageResult.of(result);
    }
}
