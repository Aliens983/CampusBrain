package com.laoliu.cas.system.interfaces.controller.app;

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
 * 邮件发送接口（扁平路径，供前端直接调用）。
 *
 * @author forever-king
 */
@Tag(name = "邮件发送（用户）")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class EmailController {

    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "发送验证码邮件", description = "向指定邮箱发送验证码，60 秒内只能发送一次，Redis 缓存 5 分钟")
    @PostMapping("/verification-code")
    public CommonResult<EmailResponse> sendEmail(@Valid @RequestBody EmailRequest request) {
        emailVerificationService.sendVerificationCode(request.getTo());
        return CommonResult.success(EmailResponse.builder().message("邮件发送成功").build());
    }
}
