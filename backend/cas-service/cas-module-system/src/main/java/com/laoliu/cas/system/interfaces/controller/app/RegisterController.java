package com.laoliu.cas.system.interfaces.controller.app;

import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.system.application.service.AuthService;
import com.laoliu.cas.system.interfaces.dto.request.UserRegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 注册接口（扁平路径，供前端直接调用）
 *
 * @author forever-king
 */
@Tag(name = "用户注册（用户）")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class RegisterController {

    private final AuthService authService;

    @Operation(summary = "验证邮箱验证码并注册", description = "用户通过邮箱验证码完成注册流程，验证成功后自动创建用户并返回用户ID")
    @PostMapping("/register")
    public CommonResult<Long> verifyEmailCode(@Valid @RequestBody UserRegisterRequest request) {
        // 入参 DTO 与应用层契约已统一为 UserRegisterRequest，无需逐字段拷贝（7.3.3）
        Long userId = authService.register(request);
        return CommonResult.success(userId);
    }
}
