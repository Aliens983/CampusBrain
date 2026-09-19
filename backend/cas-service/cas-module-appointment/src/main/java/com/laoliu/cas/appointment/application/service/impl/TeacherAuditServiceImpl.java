package com.laoliu.cas.appointment.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.AuditSource;
import com.laoliu.cas.appointment.application.service.ServiceStatusService;
import com.laoliu.cas.appointment.application.service.TeacherAuditService;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.domain.view.BookingQueryView;
import com.laoliu.cas.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 教师审核服务实现 —— 归属判定：预约单所约咨询师绑定的账号(c.user_id) == 当前教师
 *
 * @author forever-king
 */
@Service
@RequiredArgsConstructor
public class TeacherAuditServiceImpl implements TeacherAuditService {

    private final BookingRepository bookingRepository;
    private final ServiceStatusService serviceStatusService;

    @Override
    public IPage<BookingQueryView> listMyBookings(Long teacherId, int page, int pageSize, Integer manageStatus) {
        return bookingRepository.getTeacherBookings(teacherId, page, pageSize, manageStatus);
    }

    @Override
    public void approve(Long teacherId, Long orderId, String reason) {
        assertOwner(teacherId, orderId);
        serviceStatusService.auditPass(orderId, reason, AuditSource.TEACHER);
    }

    @Override
    public void reject(Long teacherId, Long orderId, String reason) {
        assertOwner(teacherId, orderId);
        serviceStatusService.auditReject(orderId, reason, AuditSource.TEACHER);
    }

    private void assertOwner(Long teacherId, Long orderId) {
        Long owner = bookingRepository.selectConsultantOwnerByOrderId(orderId);
        if (owner == null || !owner.equals(teacherId)) {
            throw new ForbiddenException(403, "无权审核该申请（仅限您名下咨询档期的预约）");
        }
    }
}
