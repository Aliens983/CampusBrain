package com.laoliu.cas.appointment.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.ServiceService;
import com.laoliu.cas.appointment.domain.entity.Service;
import com.laoliu.cas.appointment.domain.repository.ServiceRepository;
import com.laoliu.cas.appointment.interfaces.dto.request.ServiceAddRequest;
import com.laoliu.cas.appointment.interfaces.dto.request.ServicePageReqVO;
import com.laoliu.cas.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Optional;

/**
 * 服务管理应用服务实现
 *
 * @author forever-king
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceServiceImpl implements ServiceService {

    private final ServiceRepository serviceRepository;

    @Override
    public List<Service> getAllServices() {
        return serviceRepository.findAll();
    }

    @Override
    public PageResult<Service> getAllServices(ServicePageReqVO reqVO) {
        IPage<Service> result = serviceRepository.findAll(
                reqVO.getPageNo(), reqVO.getPageSize(),
                reqVO.getServiceName(), reqVO.getServiceState());
        return PageResult.of(result);
    }

    @Override
    @Cacheable(value = "services", key = "'available'")
    public List<Service> getAvailableServices() {
        return serviceRepository.findAll().stream()
                .filter(Service::isAvailable)
                .toList();
    }

    @Override
    @Cacheable(value = "services", key = "#id")
    public Optional<Service> getServiceById(Long id) {
        return serviceRepository.findById(id);
    }

    private static final java.util.Set<String> CATEGORIES =
            java.util.Set.of("teacher", "equipment", "space", "activity", "other");

    @Override
    @CacheEvict(value = "services", allEntries = true)
    public boolean addService(ServiceAddRequest request) {
        Service service = Service.builder()
                .serviceName(request.getServiceName())
                .serviceDescribe(request.getServiceDescribe())
                .serviceState(request.getServiceState() == null ? 1 : request.getServiceState())
                .category(normalizeCategory(request.getCategory()))
                .campus(normalizeCampus(request.getCampus()))
                .capacity(request.getCapacity() == null ? -1 : request.getCapacity())
                .imageUrl(request.getImageUrl())
                .build();
        serviceRepository.save(service);
        return true;
    }

    @Override
    @CacheEvict(value = "services", allEntries = true)
    public boolean updateService(Long id, ServiceAddRequest request) {
        Service existing = serviceRepository.findById(id).orElse(null);
        if (existing == null) return false;
        existing.setServiceName(request.getServiceName());
        existing.setServiceDescribe(request.getServiceDescribe());
        if (request.getServiceState() != null) existing.setServiceState(request.getServiceState());
        // 分类、校区创建后不可改（避免资源归属错位）；容量、封面可更新
        if (request.getCapacity() != null) existing.setCapacity(request.getCapacity());
        if (request.getImageUrl() != null) existing.setImageUrl(request.getImageUrl());
        serviceRepository.save(existing);
        return true;
    }

    /** 分类白名单，非法分类回退 other */
    private String normalizeCategory(String category) {
        if (category != null && CATEGORIES.contains(category)) {
            return category;
        }
        return "other";
    }

    /** 校区仅支持 cq/xs，非法回退 cq */
    private String normalizeCampus(String campus) {
        return "xs".equals(campus) ? "xs" : "cq";
    }

    @Override
    public List<Service> selectUserServices(Long userId) {
        return serviceRepository.findByUserId(userId);
    }

    @Override
    public PageResult<Service> selectUserServices(Long userId, int page, int pageSize) {
        IPage<Service> result = serviceRepository.findByUserId(userId, page, pageSize);
        return PageResult.of(result);
    }
}
