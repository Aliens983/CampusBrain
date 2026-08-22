package com.laoliu.cas.appointment.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laoliu.cas.appointment.domain.entity.Consultant;
import com.laoliu.cas.appointment.domain.repository.ConsultantRepository;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ConsultantDO;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.ConsultantMapper;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.TimeSlotMapper;
import com.laoliu.cas.appointment.interfaces.dto.response.TimeSlotRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 咨询师仓库实现
 *
 * @author forever-king
 */
@Repository
@RequiredArgsConstructor
public class ConsultantRepositoryImpl implements ConsultantRepository {

    private final ConsultantMapper consultantMapper;
    private final TimeSlotMapper timeSlotMapper;

    @Override
    public Optional<Consultant> findById(Long id) {
        return Optional.ofNullable(consultantMapper.selectById(id))
                .map(ConsultantDO::toEntity);
    }

    @Override
    public List<Consultant> findByServiceId(Long serviceId) {
        return consultantMapper.findByServiceId(serviceId).stream()
                .map(ConsultantDO::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Consultant> findAll() {
        return consultantMapper.selectList(null).stream()
                .map(ConsultantDO::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<Consultant> findPage(int page, int pageSize, String name, String department) {
        LambdaQueryWrapper<ConsultantDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(ConsultantDO::getName, name);
        }
        if (StringUtils.hasText(department)) {
            wrapper.like(ConsultantDO::getDepartment, department);
        }
        wrapper.orderByDesc(ConsultantDO::getRating);

        Page<ConsultantDO> pageParam = new Page<>(page, pageSize);
        IPage<ConsultantDO> doPage = consultantMapper.selectPage(pageParam, wrapper);
        return doPage.convert(ConsultantDO::toEntity);
    }

    @Override
    public List<TimeSlotRespVO> findTimeSlots(Long consultantId, String date) {
        return timeSlotMapper.findAvailableByConsultantAndDate(consultantId, date).stream()
                .map(slot -> TimeSlotRespVO.builder()
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .available("true")
                        .build())
                .collect(Collectors.toList());
    }
}
