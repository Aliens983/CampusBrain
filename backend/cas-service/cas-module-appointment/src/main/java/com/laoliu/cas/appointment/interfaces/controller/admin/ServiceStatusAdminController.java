package com.laoliu.cas.appointment.interfaces.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.application.service.ServiceStatusService;
import com.laoliu.cas.appointment.interfaces.dto.request.AuditRequest;
import com.laoliu.cas.appointment.interfaces.dto.request.ServiceStatusPageReqVO;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;
import com.laoliu.cas.common.annotation.RequireRole;
import com.laoliu.cas.common.enums.ManageStatus;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.exception.code.BookErrorCode;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.common.result.PageResult;
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

/**
 * 管理员端服务审核接口。
 *
 * @author forever-king
 */
@Tag(name = "预约审核（管理）")
@RestController
@RequestMapping("/admin/bookings")
@RequiredArgsConstructor
public class ServiceStatusAdminController {

    private final ServiceStatusService serviceStatusService;

    @Operation(summary = "获取所有预约记录（分页+筛选）", description = "管理员分页获取所有用户的预约记录，支持按审核状态和服务名称筛选")
    @GetMapping
    @RequireRole({UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<PageResult<ServiceStatusResponse>> getAllBookings(@Valid ServiceStatusPageReqVO reqVO) {
        IPage<ServiceStatusResponse> statusPage = serviceStatusService.getServiceStatus(reqVO);
        return CommonResult.success(PageResult.of(statusPage));
    }

    @Operation(summary = "审核通过预约", description = "管理员审核通过用户的预约申请，审核通过后发送邮件通知申请人")
    @PatchMapping("/{id}/approve")
    @RequireRole({UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<Void> auditPass(@PathVariable Long id, @Valid @RequestBody AuditRequest auditRequest) {
        if (auditRequest.getStatus() == null || auditRequest.getStatus() != ManageStatus.APPROVED.getCode()) {
            return CommonResult.error(BookErrorCode.INVALID_AUDIT_STATUS);
        }
        serviceStatusService.auditPass(id, auditRequest.getReason());
        return CommonResult.success("审核通过成功", null);
    }

    @Operation(summary = "审核不通过预约", description = "管理员审核拒绝用户的预约申请，需要填写拒绝原因，审核驳回后发送邮件通知申请人")
    @PatchMapping("/{id}/reject")
    @RequireRole({UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<Void> auditReject(@PathVariable Long id, @Valid @RequestBody AuditRequest auditRequest) {
        if (auditRequest.getStatus() == null || auditRequest.getStatus() != ManageStatus.REJECTED.getCode()) {
            return CommonResult.error(BookErrorCode.INVALID_AUDIT_STATUS);
        }
        if (auditRequest.getReason() == null || auditRequest.getReason().trim().isEmpty()) {
            return CommonResult.error(BookErrorCode.AUDIT_REASON_REQUIRED);
        }
        serviceStatusService.auditReject(id, auditRequest.getReason());
        return CommonResult.success("审核驳回成功", null);
    }
}
