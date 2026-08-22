package com.laoliu.cas.appointment.domain.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.domain.entity.Consultant;
import com.laoliu.cas.appointment.interfaces.dto.response.TimeSlotRespVO;

import java.util.List;
import java.util.Optional;

/**
 * 咨询师领域仓库接口
 *
 * @author forever-king
 */
public interface ConsultantRepository {

    Optional<Consultant> findById(Long id);

    List<Consultant> findByServiceId(Long serviceId);

    List<Consultant> findAll();

    /** 分页查询咨询师，支持按名称/部门筛选 */
    IPage<Consultant> findPage(int page, int pageSize, String name, String department);

    /** 查询咨询师某日可用时段 */
    List<TimeSlotRespVO> findTimeSlots(Long consultantId, String date);
}
