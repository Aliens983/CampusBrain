package com.laoliu.cas.appointment.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;

/**
 * 教师审核服务 —— 教师审「自己名下咨询档期」的学生申请
 *
 * @author forever-king
 */
public interface TeacherAuditService {

    /** 我名下咨询档期的申请列表（manageStatus 可空=全部） */
    IPage<ServiceStatusResponse> listMyBookings(Long teacherId, int page, int pageSize, Integer manageStatus);

    /** 审核通过（仅限本人名下咨询档期） */
    void approve(Long teacherId, Long orderId, String reason);

    /** 审核拒绝（仅限本人名下咨询档期） */
    void reject(Long teacherId, Long orderId, String reason);
}
