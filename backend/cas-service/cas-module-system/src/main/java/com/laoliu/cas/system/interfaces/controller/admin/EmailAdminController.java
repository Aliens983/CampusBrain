package com.laoliu.cas.system.interfaces.controller.admin;

import com.laoliu.cas.common.annotation.RequireRole;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.system.application.service.EmailVerificationService;
import com.laoliu.cas.system.interfaces.dto.request.EmailRequest;
import com.laoliu.cas.system.interfaces.dto.response.EmailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员端邮件发送接口
 *
 * @author forever-king
 */
@Tag(name = "邮件发送（管理）")
@RestController
@RequestMapping("/admin/email")
@RequiredArgsConstructor
public class EmailAdminController {

    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "发送验证码邮件", description = "向指定邮箱发送验证码，60 秒内只能发送一次，Redis 缓存 5 分钟")
    @PostMapping
    @RequireRole({UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<EmailResponse> sendEmail(@Valid @RequestBody EmailRequest request) {
        emailVerificationService.sendVerificationCode(request.getTo());
        return CommonResult.success(EmailResponse.builder().message("邮件发送成功").build());
    }
}
