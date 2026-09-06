package com.laoliu.cas.appointment.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laoliu.cas.appointment.domain.entity.ServiceCategory;
import com.laoliu.cas.appointment.domain.repository.ServiceCategoryRepository;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ServiceCategoryDO;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.ServiceCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 服务业务分类仓库实现 - 基础设施层（只读）
 *
 * @author forever-king
 */
@Repository
@RequiredArgsConstructor
public class ServiceCategoryRepositoryImpl implements ServiceCategoryRepository {

    private final ServiceCategoryMapper serviceCategoryMapper;

    @Override
    public List<ServiceCategory> findAll() {
        return serviceCategoryMapper.selectList(
                        new LambdaQueryWrapper<ServiceCategoryDO>()
                                .orderByAsc(ServiceCategoryDO::getSort)
                                .orderByAsc(ServiceCategoryDO::getId))
                .stream()
                .map(ServiceCategoryDO::toEntity)
                .toList();
    }
}
