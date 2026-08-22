package com.laoliu.cas.appointment.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laoliu.cas.appointment.domain.entity.Equipment;
import com.laoliu.cas.appointment.domain.repository.EquipmentRepository;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.EquipmentDO;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.EquipmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 设备仓库实现
 *
 * @author forever-king
 */
@Repository
@RequiredArgsConstructor
public class EquipmentRepositoryImpl implements EquipmentRepository {

    private final EquipmentMapper equipmentMapper;

    @Override
    public Optional<Equipment> findById(Long id) {
        return Optional.ofNullable(equipmentMapper.selectById(id))
                .map(EquipmentDO::toEntity);
    }

    @Override
    public List<Equipment> findByServiceId(Long serviceId) {
        return equipmentMapper.findByServiceId(serviceId).stream()
                .map(EquipmentDO::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Equipment> findAll() {
        return equipmentMapper.selectList(null).stream()
                .map(EquipmentDO::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<Equipment> findPage(int page, int pageSize, String name, String category) {
        LambdaQueryWrapper<EquipmentDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(EquipmentDO::getName, name);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(EquipmentDO::getCategory, category);
        }
        wrapper.orderByAsc(EquipmentDO::getId);

        Page<EquipmentDO> pageParam = new Page<>(page, pageSize);
        IPage<EquipmentDO> doPage = equipmentMapper.selectPage(pageParam, wrapper);
        return doPage.convert(EquipmentDO::toEntity);
    }

    @Override
    public List<String> findDistinctCategories() {
        return equipmentMapper.findDistinctCategories();
    }
}
