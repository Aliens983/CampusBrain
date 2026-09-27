package com.laoliu.cas.appointment.application.service.impl;

import com.laoliu.cas.appointment.application.dto.response.ScheduleCancelRequestResponse;
import com.laoliu.cas.appointment.application.dto.response.TeacherScheduleSlotResponse;
import com.laoliu.cas.appointment.application.service.TeacherScheduleService;
import com.laoliu.cas.appointment.domain.entity.Consultant;
import com.laoliu.cas.appointment.domain.entity.ScheduleCancelRequest;
import com.laoliu.cas.appointment.domain.entity.TimeSlot;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.domain.repository.ConsultantRepository;
import com.laoliu.cas.appointment.domain.repository.ScheduleCancelRepository;
import com.laoliu.cas.appointment.domain.repository.TimeSlotRepository;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TeacherScheduleServiceImpl implements TeacherScheduleService {

    private static final int PENDING = 0;
    private static final int APPROVED = 1;
    private static final int REJECTED = 2;
    private static final int AVAILABLE = 1;

    private final ConsultantRepository consultantRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final ScheduleCancelRepository scheduleCancelRepository;
    private final BookingRepository bookingRepository;

    @Override
    public List<TeacherScheduleSlotResponse> getMySchedule(Long teacherUserId, LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now() : from;
        LocalDate end = to == null ? start.plusDays(13) : to;
        if (end.isBefore(start) || end.isAfter(start.plusDays(30))) {
            throw new BusinessException(400, "日期范围无效，最多查询31天");
        }
        List<Consultant> consultants = requireTeacherConsultants(teacherUserId);
        List<Long> consultantIds = consultants.stream().map(Consultant::getId).toList();
        Map<Long, String> names = new HashMap<>();
        consultants.forEach(c -> names.put(c.getId(), c.getName()));
        Map<Long, Long> pendingBySlot = new HashMap<>();
        for (ScheduleCancelRequest request : scheduleCancelRepository.findByTeacher(teacherUserId)) {
            if (request.getStatus() == PENDING) pendingBySlot.put(request.getSlotId(), request.getId());
        }
        List<TeacherScheduleSlotResponse> result = new ArrayList<>();
        for (TimeSlot slot : timeSlotRepository.findByConsultantsAndDateRange(consultantIds, start, end)) {
            Integer rawStatus = slot.getRawAvailability() == null ? 0 : slot.getRawAvailability();
            result.add(TeacherScheduleSlotResponse.builder()
                    .slotId(slot.getId()).consultantId(slot.getConsultantId())
                    .consultantName(names.get(slot.getConsultantId())).date(slot.getSlotDate())
                    .startTime(slot.getStartTime()).endTime(slot.getEndTime()).availability(rawStatus)
                    .pendingRequestId(pendingBySlot.get(slot.getId())).build());
        }
        return result;
    }

    @Override
    public List<ScheduleCancelRequestResponse> getMyRequests(Long teacherUserId) {
        return scheduleCancelRepository.findByTeacher(teacherUserId).stream()
                .map(request -> toResponse(request, null, null)).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyCancellation(Long teacherUserId, Long slotId, String reason) {
        if (!StringUtils.hasText(reason) || reason.trim().length() > 255) {
            throw new BusinessException(400, "停诊事由必填且不能超过255个字符");
        }
        List<Consultant> consultants = requireTeacherConsultants(teacherUserId);
        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException(400, "排班时段不存在"));
        Consultant owner = consultants.stream().filter(c -> c.getId().equals(slot.getConsultantId())).findFirst()
                .orElseThrow(() -> new ForbiddenException(403, "不能操作其他教师的排班"));
        if (!slot.getSlotDate().isAfter(LocalDate.now())) {
            throw new BusinessException(400, "只能申请取消未来日期的排班");
        }
        if (slotAvailability(slotId) != AVAILABLE) {
            throw new BusinessException(400, "该时段已占用或已停诊，不能申请");
        }
        if (bookingRepository.countActiveConsultationBookingsBySlot(slotId) > 0) {
            throw new BusinessException(400, "该时段已有未完成预约，不能申请停诊");
        }
        if (scheduleCancelRepository.countPendingForSlot(slotId) > 0) {
            throw new BusinessException(400, "该时段已有待审批申请");
        }
        try {
            scheduleCancelRepository.insert(ScheduleCancelRequest.builder()
                    .consultantId(owner.getId()).slotId(slot.getId()).slotDate(slot.getSlotDate())
                    .startTime(slot.getStartTime()).endTime(slot.getEndTime()).reason(reason.trim())
                    .status(PENDING).teacherUserId(teacherUserId).build());
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 双击/并发提交：上方 count 检查与 INSERT 之间存在窗口，由 DB 唯一键
            // uk_scr_pending_slot 兜底，此处转成业务 400 而不是让唯一键冲突冒成 500
            throw new BusinessException(400, "该时段已有待审批申请，请勿重复提交");
        }
    }

    @Override
    public List<ScheduleCancelRequestResponse> getPendingRequests() {
        return scheduleCancelRepository.findPending().stream().map(request -> toResponse(request, null, null)).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long requestId, Long auditorId, String remark) {
        ScheduleCancelRequest request = scheduleCancelRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BusinessException(400, "停诊申请不存在"));
        request = scheduleCancelRepository.findPendingBySlotForUpdate(request.getSlotId())
                .filter(pending -> pending.getId().equals(requestId))
                .orElseThrow(() -> new BusinessException(400, "该时段申请已处理或不存在"));
        if (request.getStatus() != PENDING) throw new BusinessException(400, "该申请已处理");
        TimeSlot slot = timeSlotRepository.findByIdForUpdate(request.getSlotId())
                .orElseThrow(() -> new BusinessException(400, "排班时段不存在"));
        if (timeSlotRepository.findAvailabilityForUpdate(request.getSlotId()).orElse(0) != AVAILABLE
                || bookingRepository.countActiveConsultationBookingsBySlot(request.getSlotId()) > 0) {
            throw new BusinessException(400, "该时段已被预约或不可停诊");
        }
        if (!timeSlotRepository.markCancelledIfAvailable(request.getSlotId())) {
            throw new BusinessException(400, "时段状态已变化，请刷新后重试");
        }
        if (!scheduleCancelRepository.approve(requestId, auditorId, cleanRemark(remark))) {
            throw new BusinessException(400, "申请状态已变化，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long requestId, Long auditorId, String remark) {
        ScheduleCancelRequest request = scheduleCancelRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BusinessException(400, "停诊申请不存在"));
        if (request.getStatus() != PENDING) throw new BusinessException(400, "该申请已处理");
        if (!scheduleCancelRepository.reject(requestId, auditorId, cleanRemark(remark))) {
            throw new BusinessException(400, "申请状态已变化，请刷新后重试");
        }
    }

    private List<Consultant> requireTeacherConsultants(Long teacherUserId) {
        List<Consultant> consultants = consultantRepository.findTeacherConsultantsByUserId(teacherUserId);
        if (consultants.isEmpty()) throw new ForbiddenException(403, "当前教师账号未绑定教师咨询档期");
        return consultants;
    }

    private int slotAvailability(Long slotId) {
        return timeSlotRepository.findAvailability(slotId).orElse(0);
    }

    private String cleanRemark(String remark) {
        if (!StringUtils.hasText(remark)) return null;
        String normalized = remark.trim();
        if (normalized.length() > 255) throw new BusinessException(400, "审批意见不能超过255个字符");
        return normalized;
    }

    private ScheduleCancelRequestResponse toResponse(ScheduleCancelRequest request, String consultantName, String teacherName) {
        if (consultantName == null) {
            consultantName = request.getConsultantName() == null
                    ? consultantRepository.findById(request.getConsultantId()).map(Consultant::getName).orElse("")
                    : request.getConsultantName();
        }
        return ScheduleCancelRequestResponse.builder().id(request.getId())
                .consultantId(request.getConsultantId()).consultantName(consultantName)
                .slotId(request.getSlotId()).date(request.getSlotDate()).startTime(request.getStartTime())
                .endTime(request.getEndTime()).reason(request.getReason()).status(request.getStatus())
                .teacherUserId(request.getTeacherUserId())
                .teacherName(teacherName == null ? request.getTeacherName() : teacherName)
                .auditorId(request.getAuditorId()).auditRemark(request.getAuditRemark())
                .auditTime(request.getAuditTime()).createTime(request.getCreateTime()).build();
    }
}
