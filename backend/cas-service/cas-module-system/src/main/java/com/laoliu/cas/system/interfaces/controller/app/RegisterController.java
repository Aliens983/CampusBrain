package com.laoliu.cas.system.interfaces.controller.app;

import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.system.application.service.AuthService;
import com.laoliu.cas.system.application.service.vo.UserRegisterVO;
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
        UserRegisterVO vo = new UserRegisterVO();
        vo.setName(request.getName());
        vo.setGrade(request.getGrade());
        vo.setSex(request.getSex());
        vo.setAge(request.getAge());
        vo.setEmail(request.getEmail());
        vo.setCode(request.getCode());
        vo.setPassword(request.getPassword());

        Long userId = authService.register(vo);
        return CommonResult.success(userId);
    }
}
