package com.laoliu.cas.appointment.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laoliu.cas.appointment.domain.entity.Service;
import com.laoliu.cas.appointment.domain.repository.ServiceRepository;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ServicesDO;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.ServiceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 服务仓库实现 - 基础设施层。
 *
 * @author forever-king
 */
@Repository
@RequiredArgsConstructor
public class ServiceRepositoryImpl implements ServiceRepository {

    private final ServiceMapper serviceMapper;

    @Override
    public Optional<Service> findById(Long id) {
        ServicesDO dataObject = serviceMapper.selectById(id);
        return Optional.ofNullable(dataObject).map(ServicesDO::toEntity);
    }

    @Override
    public List<Service> findAll() {
        return serviceMapper.selectList(null).stream()
                .map(ServicesDO::toEntity)
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
        return doPage.convert(ServicesDO::toEntity);
    }

    @Override
    public List<Service> findByUserId(Long userId) {
        return serviceMapper.selectUserServices(userId).stream()
                .map(ServicesDO::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<Service> findByUserId(Long userId, int page, int pageSize) {
        Page<ServicesDO> pageParam = new Page<>(page, pageSize);
        IPage<ServicesDO> doPage = serviceMapper.selectUserServicesWithPage(userId, pageParam);
        return doPage.convert(ServicesDO::toEntity);
    }

    @Override
    public Service save(Service service) {
        ServicesDO dataObject = ServicesDO.fromEntity(service);
        if (service.getServiceId() == null) {
            serviceMapper.insert(dataObject);
        } else {
            serviceMapper.updateById(dataObject);
        }
        return dataObject.toEntity();
    }

    @Override
    public void deleteById(Long id) {
        serviceMapper.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return serviceMapper.selectById(id) != null;
    }
}
