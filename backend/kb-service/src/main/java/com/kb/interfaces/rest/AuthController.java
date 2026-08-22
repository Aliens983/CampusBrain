package com.kb.interfaces.rest;

import com.kb.domain.user.KbUser;
import com.kb.domain.user.UserRepository;
import com.kb.infrastructure.common.BusinessException;
import com.kb.infrastructure.common.ErrorCode;
import com.kb.infrastructure.persistence.mysql.dataobject.OAuth2AccessTokenDO;
import com.kb.infrastructure.security.EmailVerificationService;
import com.kb.infrastructure.security.OAuth2TokenService;
import com.kb.infrastructure.security.SecurityFrameworkUtils;
import com.kb.interfaces.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 — 登录 / 注册 / 刷新 / 登出
 *
 * @author forever-king
 */
@Tag(name = "认证管理", description = "登录、注册、Token 刷新、登出等认证相关接口")
@RestController
@RequestMapping("/kb/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OAuth2TokenService tokenService;
    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回 Access Token 和 Refresh Token")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        KbUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException.AuthenticationException(
                        ErrorCode.AUTH_BAD_CREDENTIALS));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException.AuthenticationException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }
        OAuth2AccessTokenDO tokenDO = tokenService.createTokens(
                user.getId(), user.getUsername(), user.getRole(), user.getNickname());
        return ApiResponse.success(LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .nickname(user.getNickname())
                .accessToken(tokenDO.getAccessToken())
                .refreshToken(tokenDO.getRefreshToken())
                .expiresTime(tokenDO.getExpiresTime())
                .build());
    }

    @Operation(summary = "刷新 Access Token", description = "使用 Refresh Token 获取新的 Access Token")
    @PostMapping("/refresh-token")
    public ApiResponse<LoginResponse> refreshToken(
            @Parameter(description = "刷新令牌") @RequestParam String refreshToken) {
        OAuth2AccessTokenDO tokenDO = tokenService.refreshAccessToken(refreshToken);
        return ApiResponse.success(LoginResponse.builder()
                .userId(tokenDO.getUserId())
                .role(tokenDO.getRole())
                .accessToken(tokenDO.getAccessToken())
                .refreshToken(refreshToken)
                .expiresTime(tokenDO.getExpiresTime())
                .build());
    }

    @Operation(summary = "发送邮箱验证码", description = "向指定邮箱发送 6 位数字验证码，用于注册")
    @PostMapping("/send-verification-code")
    public ApiResponse<Void> sendVerificationCode(@Valid @RequestBody EmailRequest request) {
        emailVerificationService.sendVerificationCode(request.getTo());
        return ApiResponse.success();
    }

    @Operation(summary = "邮箱验证码注册", description = "使用邮箱验证码注册新账号，成功后返回双 Token")
    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        // 防用户枚举：统一错误信息，不区分用户名/邮箱冲突原因
        boolean usernameExists = userRepository.findByUsername(request.getUsername()).isPresent();
        boolean emailExists = userRepository.findByEmail(request.getEmail()).isPresent();
        if (usernameExists || emailExists) {
            throw new BusinessException.UserException(ErrorCode.BAD_REQUEST,
                    "注册失败，请检查输入信息");
        }
        if (!emailVerificationService.verifyCode(request.getEmail(), request.getCode())) {
            throw new BusinessException.UserException(ErrorCode.USER_VERIFICATION_CODE_INVALID);
        }

        KbUser user = KbUser.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .nickname(request.getUsername())
                .enabled(true)
                .build();
        user = userRepository.save(user);

        OAuth2AccessTokenDO tokenDO = tokenService.createTokens(
                user.getId(), user.getUsername(), user.getRole(), user.getNickname());
        return ApiResponse.success(LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .nickname(user.getNickname())
                .accessToken(tokenDO.getAccessToken())
                .refreshToken(tokenDO.getRefreshToken())
                .expiresTime(tokenDO.getExpiresTime())
                .build());
    }

    @Operation(summary = "用户登出", description = "删除 Access Token，使当前会话失效")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @Parameter(description = "Bearer Token") @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            tokenService.removeAccessToken(authHeader.substring(7));
        }
        SecurityFrameworkUtils.clearContext();
        return ApiResponse.success();
    }
}
