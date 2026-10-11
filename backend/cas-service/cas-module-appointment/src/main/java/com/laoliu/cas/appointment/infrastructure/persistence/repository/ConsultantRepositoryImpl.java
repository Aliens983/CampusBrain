package com.laoliu.cas.appointment.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.CommonErrorCode;
import com.laoliu.cas.appointment.domain.entity.Consultant;
import com.laoliu.cas.appointment.domain.repository.ConsultantRepository;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ConsultantDO;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.ConsultantMapper;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.TimeSlotMapper;
import com.laoliu.cas.appointment.domain.view.TimeSlotView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
    public List<Consultant> findActiveTeacherConsultants() {
        return consultantMapper.findActiveTeacherConsultants().stream()
                .map(ConsultantDO::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Consultant> findTeacherConsultantsByUserId(Long userId) {
        return consultantMapper.findTeacherConsultantsByUserId(userId).stream()
                .map(ConsultantDO::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<Consultant> findPage(int page, int pageSize, String name, String department, Long serviceId) {
        LambdaQueryWrapper<ConsultantDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(ConsultantDO::getName, name);
        }
        if (StringUtils.hasText(department)) {
            wrapper.like(ConsultantDO::getDepartment, department);
        }
        if (serviceId != null) {
            wrapper.eq(ConsultantDO::getServiceId, serviceId);
        }
        wrapper.orderByDesc(ConsultantDO::getRating);

        Page<ConsultantDO> pageParam = new Page<>(page, pageSize);
        IPage<ConsultantDO> doPage = consultantMapper.selectPage(pageParam, wrapper);
        return doPage.convert(ConsultantDO::toEntity);
    }

    @Override
    public List<TimeSlotView> findTimeSlots(Long consultantId, String date) {
        // 该查询参数不经过请求 DTO 的 Bean Validation，非法日期必须在此兜底：
        // 否则 DateTimeParseException 会穿透成 500（正确语义应为 400 参数错误）.
        LocalDate day;
        if (date == null || date.isBlank()) {
            day = LocalDate.now();
        } else {
            try {
                day = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                // 用 (code, message) 构造：BAD_REQUEST 的消息无 %s 占位符，
                // (ErrorCode, args) 构造会吞掉这里的具体提示
                throw new BusinessException(CommonErrorCode.BAD_REQUEST.getCode(),
                        "日期格式必须为 yyyy-MM-dd");
            }
        }
        return timeSlotMapper.findAvailableByConsultantAndDate(consultantId, day).stream()
                .map(slot -> TimeSlotView.builder()
                        .slotId(slot.getId())
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .available("true")
                        .build())
                .collect(Collectors.toList());
    }
}
