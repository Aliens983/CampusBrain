package com.laoliu.cas.appointment.interfaces.controller.teacher;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.TeacherAuditService;
import com.laoliu.cas.appointment.interfaces.dto.request.TeacherAuditRequest;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;
import com.laoliu.cas.common.annotation.RequireRole;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.common.result.PageResult;
import com.laoliu.cas.common.security.SecurityFrameworkUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端预约审核接口 —— 教师仅能审「自己名下咨询档期」的申请（越权抛 403）
 *
 * @author forever-king
 */
@Tag(name = "预约审核（教师）")
@RestController
@RequestMapping("/teacher/bookings")
@RequiredArgsConstructor
public class TeacherAuditController {

    private final TeacherAuditService teacherAuditService;

    @Operation(summary = "我名下咨询档期的申请列表", description = "status 可空=全部；0=待我审核")
    @GetMapping
    @RequireRole({UserRoleEnum.TEACHER, UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<PageResult<ServiceStatusResponse>> listMyBookings(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        Long teacherId = SecurityFrameworkUtils.getLoginUserId();
        IPage<ServiceStatusResponse> page = teacherAuditService.listMyBookings(teacherId, pageNo, pageSize, status);
        return CommonResult.success(PageResult.of(page));
    }

    @Operation(summary = "审核通过（仅本人名下咨询档期）")
    @PatchMapping("/{id}/approve")
    @RequireRole({UserRoleEnum.TEACHER, UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<Void> approve(@PathVariable Long id, @RequestBody(required = false) TeacherAuditRequest request) {
        Long teacherId = SecurityFrameworkUtils.getLoginUserId();
        String reason = request == null ? null : request.getReason();
        teacherAuditService.approve(teacherId, id, reason);
        return CommonResult.success("审核通过成功", null);
    }

    @Operation(summary = "审核拒绝（仅本人名下咨询档期，拒绝必填原因）")
    @PatchMapping("/{id}/reject")
    @RequireRole({UserRoleEnum.TEACHER, UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<Void> reject(@PathVariable Long id, @RequestBody TeacherAuditRequest request) {
        Long teacherId = SecurityFrameworkUtils.getLoginUserId();
        teacherAuditService.reject(teacherId, id, request.getReason());
        return CommonResult.success("审核驳回成功", null);
    }
}
