package com.laoliu.cas.appointment.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.ConsultationService;
import com.laoliu.cas.appointment.domain.entity.Consultant;
import com.laoliu.cas.appointment.domain.entity.Service;
import com.laoliu.cas.appointment.domain.repository.ConsultantRepository;
import com.laoliu.cas.appointment.domain.repository.ServiceRepository;
import com.laoliu.cas.appointment.interfaces.dto.response.ConsultantResponse;
import com.laoliu.cas.appointment.interfaces.dto.response.TimeSlotRespVO;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 咨询查询应用服务实现 — 全部从数据库读取真实数据
 *
 * @author forever-king
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final ServiceRepository serviceRepository;
    private final ConsultantRepository consultantRepository;

    private static final List<String> CONSULTATION_KEYWORDS = Arrays.asList(
            "咨询", "辅导", "指导", "心理"
    );

    @Override
    public List<ConsultantResponse> getAvailableConsultants() {
        List<Long> consultationServiceIds = getConsultationServiceIds();
        if (consultationServiceIds.isEmpty()) {
            return Collections.emptyList();
        }
        return consultationServiceIds.stream()
                .flatMap(serviceId -> consultantRepository.findByServiceId(serviceId).stream())
                .map(this::toConsultantResponse)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询咨询师，支持按名称/部门筛选
     */
    public IPage<ConsultantResponse> getAvailableConsultants(int page, int pageSize, String name, String department) {
        List<Long> consultationServiceIds = getConsultationServiceIds();
        if (consultationServiceIds.isEmpty()) {
            return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        }
        IPage<Consultant> consultantPage = consultantRepository.findPage(page, pageSize, name, department);
        return consultantPage.convert(this::toConsultantResponse);
    }

    @Override
    public ConsultantResponse getConsultantById(Long id) {
        return consultantRepository.findById(id)
                .map(this::toConsultantResponse)
                .orElse(null);
    }

    @Override
    public List<TimeSlotRespVO> getAvailableTimeSlots(Long consultantId, String date) {
        return consultantRepository.findTimeSlots(consultantId, date);
    }

    private List<Long> getConsultationServiceIds() {
        return serviceRepository.findAll().stream()
                .filter(Service::isAvailable)
                .filter(this::isConsultation)
                .map(Service::getServiceId)
                .collect(Collectors.toList());
    }

    private boolean isConsultation(Service service) {
        if (service.getServiceName() == null) {
            return false;
        }
        return CONSULTATION_KEYWORDS.stream()
                .anyMatch(keyword -> service.getServiceName().contains(keyword));
    }

    private ConsultantResponse toConsultantResponse(Consultant consultant) {
        return ConsultantResponse.builder()
                .id(consultant.getId())
                .name(consultant.getName())
                .title(consultant.getTitle())
                .department(consultant.getDepartment())
                .expertise(consultant.getDescription() != null
                        ? Collections.singletonList(consultant.getDescription()) : Collections.emptyList())
                .rating(consultant.getRating() != null ? consultant.getRating().doubleValue() : null)
                .reviews(consultant.getReviewCount() != null ? consultant.getReviewCount() : 0)
                .available(consultant.hasRatings())
                .avatar(consultant.getAvatarUrl() != null ? consultant.getAvatarUrl() : "")
                .build();
    }
}
