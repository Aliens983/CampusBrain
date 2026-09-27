package com.laoliu.cas.appointment.interfaces.controller.teacher;

import com.laoliu.cas.appointment.application.dto.request.ScheduleCancelApplyRequest;
import com.laoliu.cas.appointment.application.dto.response.ScheduleCancelRequestResponse;
import com.laoliu.cas.appointment.application.dto.response.TeacherScheduleSlotResponse;
import com.laoliu.cas.appointment.application.service.TeacherScheduleService;
import com.laoliu.cas.common.annotation.RequireRole;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.common.security.SecurityFrameworkUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "教师排班")
@RestController
@RequestMapping("/teacher/schedule")
@RequiredArgsConstructor
@RequireRole(UserRoleEnum.TEACHER)
public class TeacherScheduleController {
    private final TeacherScheduleService teacherScheduleService;

    @Operation(summary = "查询我的咨询排班")
    @GetMapping
    public CommonResult<List<TeacherScheduleSlotResponse>> getSchedule(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return CommonResult.success(teacherScheduleService.getMySchedule(
                SecurityFrameworkUtils.getLoginUserId(), from, to));
    }

    @Operation(summary = "查询我的停诊申请记录")
    @GetMapping("/requests")
    public CommonResult<List<ScheduleCancelRequestResponse>> getRequests() {
        return CommonResult.success(teacherScheduleService.getMyRequests(SecurityFrameworkUtils.getLoginUserId()));
    }

    @Operation(summary = "申请停诊")
    @PostMapping("/requests")
    public CommonResult<Void> apply(@Valid @RequestBody ScheduleCancelApplyRequest request) {
        teacherScheduleService.applyCancellation(SecurityFrameworkUtils.getLoginUserId(),
                request.getSlotId(), request.getReason());
        return CommonResult.success("停诊申请已提交", null);
    }
}
