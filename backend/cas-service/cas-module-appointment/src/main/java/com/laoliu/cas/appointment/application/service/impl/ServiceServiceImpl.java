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
 * 服务管理应用服务实现。
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

    @Override
    @CacheEvict(value = "services", allEntries = true)
    public boolean addService(ServiceAddRequest request) {
        Service service = Service.builder()
                .serviceName(request.getServiceName())
                .serviceDescribe(request.getServiceDescribe())
                .serviceState(request.getServiceState())
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
        serviceRepository.save(existing);
        return true;
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
