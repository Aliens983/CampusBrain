package com.laoliu.cas.appointment.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.domain.view.TimeSlotView;
import com.laoliu.cas.appointment.application.dto.response.ConsultantResponse;

import java.util.List;

/**
 * 咨询应用服务接口（咨询师/时段查询与咨询预约）
 *
 * @author forever-king
 */
public interface ConsultationService {

    /**
     * 获取所有可预约的咨询师列表
     */
    List<ConsultantResponse> getAvailableConsultants();

    /**
     * 分页获取可预约咨询师，支持按名称/部门/所属服务筛选
     */
    IPage<ConsultantResponse> getAvailableConsultants(int page, int pageSize, String name, String department, Long serviceId);

    /**
     * 根据ID获取咨询师详情
     */
    ConsultantResponse getConsultantById(Long id);

    /**
     * 获取指定咨询师的可用时段
     */
    List<TimeSlotView> getAvailableTimeSlots(Long consultantId, String date);

    /**
     * 为指定用户预约咨询师时段
     *
     * @param userId       预约用户 ID
     * @param consultantId 咨询师 ID
     * @param slotId       时段 ID（由 /slots 接口返回）
     * @return 新预约单 orderId
     */
    Long bookConsultation(Long userId, Long consultantId, Long slotId);
}
