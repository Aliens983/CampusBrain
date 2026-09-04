package com.laoliu.cas.system.interfaces.controller.admin;

import com.laoliu.cas.common.annotation.RequireRole;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.system.application.service.NotificationSettingsService;
import com.laoliu.cas.system.interfaces.dto.NotifyPolicyDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端通知策略接口（系统设置 → 通知策略）。
 * <p>
 * 全局策略：邮件/站内/短信是否启用。邮件启用与否会真实控制审核结果邮件的发送；
 * 站内/短信为预留（消息中心/短信服务建设后生效）。
 * </p>
 *
 * @author forever-king
 */
@Tag(name = "通知策略（管理）")
@RestController
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
public class NotifyPolicyAdminController {

    private final NotificationSettingsService notificationSettings;

    @Operation(summary = "获取全局通知策略")
    @GetMapping("/notify")
    @RequireRole({UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<NotifyPolicyDTO> getNotifyPolicy() {
        NotifyPolicyDTO dto = new NotifyPolicyDTO(
                notificationSettings.isEmailEnabled(),
                notificationSettings.isSmsEnabled());
        return CommonResult.success(dto);
    }

    @Operation(summary = "保存全局通知策略")
    @PutMapping("/notify")
    @RequireRole({UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<Void> saveNotifyPolicy(@RequestBody NotifyPolicyDTO dto) {
        if (dto.getEmailEnabled() != null) {
            notificationSettings.setEmailEnabled(dto.getEmailEnabled());
        }
        if (dto.getSmsEnabled() != null) {
            notificationSettings.setSmsEnabled(dto.getSmsEnabled());
        }
        return CommonResult.success("已保存", null);
    }
}
