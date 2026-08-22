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

/**
 * @author forever-king
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JWTUtils jwtUtils;
    private final PasswordUtils passwordUtils;
    private final RedisUtil redisUtil;

    /** 用户登录 */
    public String login(String email, String password) {
        if (email == null || password == null) {
            throw new BusinessException(UserErrorCode.EMAIL_OR_PASSWORD_EMPTY);
        }

        String encodePassword = userRepository.getEncodePasswordByEmail(email);
        if (encodePassword == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_EXIST);
        }
        if (passwordUtils.matches(password, encodePassword)) {
            Long userId = userRepository.getUserIdByEmail(email);
            return jwtUtils.generateToken(buildLoginUser(userId, email));
        }
        throw new BusinessException(UserErrorCode.PASSWORD_ERROR);
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
