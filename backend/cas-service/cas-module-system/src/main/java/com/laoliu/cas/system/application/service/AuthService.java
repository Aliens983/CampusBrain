package com.laoliu.cas.system.application.service;

import com.laoliu.cas.system.domain.entity.User;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.UserErrorCode;
import com.laoliu.cas.common.util.PasswordUtils;
import com.laoliu.cas.redis.util.RedisUtil;
import com.laoliu.cas.common.security.JWTUtils;
import com.laoliu.cas.common.security.LoginUser;
import com.laoliu.cas.system.application.service.vo.UserRegisterVO;
import com.laoliu.cas.system.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author forever-king
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 同一账号允许的最大连续登录失败次数，达到后临时锁定 */
    private static final int MAX_LOGIN_FAIL = 5;
    /** 达到失败阈值后的锁定时长（秒） */
    private static final long LOGIN_FAIL_LOCK_SECONDS = 15 * 60;
    /** 登录失败计数在 Redis 中的 key 前缀 */
    private static final String LOGIN_FAIL_KEY_PREFIX = "login:fail:";

    private final UserRepository userRepository;
    private final JWTUtils jwtUtils;
    private final PasswordUtils passwordUtils;
    private final RedisUtil redisUtil;
    private final CaptchaService captchaService;

    /**
     * 用户登录。
     * <p>
     * 校验顺序：失败限频 → 图形验证码 → 账号密码。
     * 图形验证码为一次性，密码错误也会作废当前验证码，需重新获取。
     */
    public String login(String email, String password, String captchaUuid, String captchaCode) {
        if (email == null || password == null) {
            throw new BusinessException(UserErrorCode.EMAIL_OR_PASSWORD_EMPTY);
        }

        // 1. 限频前置：已锁定账号直接拒绝，避免继续消耗验证码
        String failKey = LOGIN_FAIL_KEY_PREFIX + email;
        Long failCount = redisUtil.get(failKey);
        if (failCount != null && failCount >= MAX_LOGIN_FAIL) {
            throw new BusinessException(UserErrorCode.LOGIN_FAILED_TOO_MANY_TIMES);
        }

        // 2. 图形验证码校验（一次性）
        captchaService.validateCaptcha(captchaUuid, captchaCode);

        // 3. 校验账号与密码
        String encodePassword = userRepository.getEncodePasswordByEmail(email);
        if (encodePassword == null) {
            recordLoginFailure(failKey);
            throw new BusinessException(UserErrorCode.USER_NOT_EXIST);
        }
        if (passwordUtils.matches(password, encodePassword)) {
            redisUtil.delete(failKey);
            Long userId = userRepository.getUserIdByEmail(email);
            return jwtUtils.generateToken(buildLoginUser(userId, email));
        }
        recordLoginFailure(failKey);
        throw new BusinessException(UserErrorCode.PASSWORD_ERROR);
    }

    /** 记录一次登录失败：原子自增计数，首次失败时设定锁定期 */
    private void recordLoginFailure(String failKey) {
        Long count = redisUtil.increment(failKey);
        if (count != null && count == 1L) {
            redisUtil.expire(failKey, LOGIN_FAIL_LOCK_SECONDS, TimeUnit.SECONDS);
        }
    }

    /** 重置密码 */
    public String resetPassword(String email, String code, String password) {
        if (email == null || email.isEmpty()) {
            throw new BusinessException(UserErrorCode.EMAIL_EMPTY);
        }
        if (code == null || code.isEmpty()) {
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_EMPTY);
        }
        if (password == null || password.isEmpty()) {
            throw new BusinessException(UserErrorCode.PASSWORD_EMPTY);
        }

        String storedCode = redisUtil.getVerificationCode("verification_code:" + email);
        if (storedCode == null) {
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_EXPIRED);
        }
        if (!storedCode.equals(code)) {
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_ERROR);
        }

        Long userId = userRepository.getUserIdByEmail(email);
        if (userId == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_EXIST_BY_EMAIL);
        }

        String encodedPassword = passwordUtils.encode(password);
        userRepository.updatePasswordByEmail(email, encodedPassword);
        redisUtil.removeVerificationCode("verification_code:" + email);

        return jwtUtils.generateToken(buildLoginUser(userId, email));
    }

    /** 构建带角色的登录上下文，用于签发包含 role 声明的 JWT */
    private LoginUser buildLoginUser(Long userId, String email) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setEmail(email);
        String role = userRepository.getRoleByUserId(userId);
        loginUser.setRole(role == null ? UserRoleEnum.USER.getCode() : Integer.parseInt(role));
        return loginUser;
    }

    /** 用户注册 */
    public Long register(UserRegisterVO request) {
        String email = request.getEmail();
        String code = request.getCode();

        if (email == null || code == null) {
            throw new BusinessException(UserErrorCode.EMAIL_OR_CODE_EMPTY);
        }

        Long ifUserId = userRepository.getUserIdByEmail(email);
        if (ifUserId != null) {
            throw new BusinessException(UserErrorCode.USER_ALREADY_EXISTS);
        }

        String storedCode = redisUtil.getVerificationCode("verification_code:" + email);
        if (storedCode == null) {
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_EXPIRED);
        }
        if (!storedCode.equals(code)) {
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_ERROR);
        }

        redisUtil.removeVerificationCode("verification_code:" + email);

        String password = request.getPassword();
        String encodedPassword = passwordUtils.encode(password);

        User user = User.builder()
                .name(request.getName()).grade(request.getGrade())
                .sex(request.getSex()).age(request.getAge())
                .role(UserRoleEnum.USER.getCode()).email(email)
                .password(encodedPassword)
                .build();

        User savedUser = userRepository.save(user);
        return savedUser.getId();
    }
}
