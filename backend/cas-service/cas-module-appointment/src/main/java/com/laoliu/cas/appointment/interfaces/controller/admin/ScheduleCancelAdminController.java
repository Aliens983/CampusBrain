package com.laoliu.cas.appointment.interfaces.controller.admin;

import com.laoliu.cas.appointment.application.dto.request.ScheduleCancelAuditRequest;
import com.laoliu.cas.appointment.application.dto.response.ScheduleCancelRequestResponse;
import com.laoliu.cas.appointment.application.service.TeacherScheduleService;
import com.laoliu.cas.common.annotation.RequireRole;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.common.security.SecurityFrameworkUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "停诊审批")
@RestController
@RequestMapping("/admin/schedule-cancellations")
@RequiredArgsConstructor
@RequireRole({UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
public class ScheduleCancelAdminController {
    private final TeacherScheduleService teacherScheduleService;

    @Operation(summary = "查询待审批停诊申请")
    @GetMapping
    public CommonResult<List<ScheduleCancelRequestResponse>> getPending() {
        return CommonResult.success(teacherScheduleService.getPendingRequests());
    }

    @Operation(summary = "批准停诊申请")
    @PatchMapping("/{id}/approve")
    public CommonResult<Void> approve(@PathVariable Long id,
                                      @Valid @RequestBody(required = false) ScheduleCancelAuditRequest request) {
        teacherScheduleService.approve(id, SecurityFrameworkUtils.getLoginUserId(),
                request == null ? null : request.getRemark());
        return CommonResult.success("停诊申请已批准", null);
    }

    @Operation(summary = "拒绝停诊申请")
    @PatchMapping("/{id}/reject")
    public CommonResult<Void> reject(@PathVariable Long id,
                                     @Valid @RequestBody(required = false) ScheduleCancelAuditRequest request) {
        teacherScheduleService.reject(id, SecurityFrameworkUtils.getLoginUserId(),
                request == null ? null : request.getRemark());
        return CommonResult.success("停诊申请已拒绝", null);
    }
}
