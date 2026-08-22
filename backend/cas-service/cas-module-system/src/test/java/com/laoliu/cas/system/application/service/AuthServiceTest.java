package com.laoliu.cas.system.application.service;

import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.UserErrorCode;
import com.laoliu.cas.common.security.JWTUtils;
import com.laoliu.cas.common.security.LoginUser;
import com.laoliu.cas.common.util.PasswordUtils;
import com.laoliu.cas.redis.util.RedisUtil;
import com.laoliu.cas.system.application.service.vo.UserRegisterVO;
import com.laoliu.cas.system.domain.entity.User;
import com.laoliu.cas.system.domain.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("认证服务单元测试")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JWTUtils jwtUtils;

    @Mock
    private RedisUtil redisUtil;

    private MockedStatic<PasswordUtils> passwordUtilsMock;

    private AuthService authService;

    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedPasswordHash";
    private static final String TOKEN = "jwt.token.string";
    private static final Long USER_ID = 1L;
    private static final String VERIFICATION_CODE = "123456";

    @BeforeEach
    void setUp() {
        passwordUtilsMock = mockStatic(PasswordUtils.class);
        authService = new AuthService(userRepository, jwtUtils, new PasswordUtils(), redisUtil);
    }

    @AfterEach
    void tearDown() {
        passwordUtilsMock.close();
    }

    // ======================== login ========================

    @Nested
    @DisplayName("登录 - login")
    class LoginTests {

        @Test
        @DisplayName("应当成功登录并返回 JWT Token")
        void shouldLoginSuccessfully() {
            // Given
            when(userRepository.getEncodePasswordByEmail(EMAIL)).thenReturn(ENCODED_PASSWORD);
            passwordUtilsMock.when(() -> PasswordUtils.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(userRepository.getUserIdByEmail(EMAIL)).thenReturn(USER_ID);
            when(userRepository.getRoleByUserId(USER_ID)).thenReturn("0");
            when(jwtUtils.generateToken(any(LoginUser.class))).thenReturn(TOKEN);

            // When
            String result = authService.login(EMAIL, PASSWORD);

            // Then
            assertNotNull(result);
            assertEquals(TOKEN, result);
            verify(userRepository).getEncodePasswordByEmail(EMAIL);
            verify(userRepository).getUserIdByEmail(EMAIL);
            verify(jwtUtils).generateToken(any(LoginUser.class));
        }

        @Test
        @DisplayName("邮箱或密码为空时应当抛出 EMAIL_OR_PASSWORD_EMPTY 异常")
        void shouldThrowExceptionWhenEmailOrPasswordEmpty() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(null, PASSWORD));
            assertEquals(UserErrorCode.EMAIL_OR_PASSWORD_EMPTY.getCode(), exception.getCode());

            exception = assertThrows(BusinessException.class,
                    () -> authService.login(EMAIL, null));
            assertEquals(UserErrorCode.EMAIL_OR_PASSWORD_EMPTY.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("用户不存在时应当抛出 USER_NOT_EXIST 异常")
        void shouldThrowExceptionWhenUserNotExist() {
            // Given
            when(userRepository.getEncodePasswordByEmail(EMAIL)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(EMAIL, PASSWORD));
            assertEquals(UserErrorCode.USER_NOT_EXIST.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("密码错误时应当抛出 PASSWORD_ERROR 异常")
        void shouldThrowExceptionWhenPasswordError() {
            // Given
            when(userRepository.getEncodePasswordByEmail(EMAIL)).thenReturn(ENCODED_PASSWORD);
            passwordUtilsMock.when(() -> PasswordUtils.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(EMAIL, PASSWORD));
            assertEquals(UserErrorCode.PASSWORD_ERROR.getCode(), exception.getCode());
        }
    }

    // ======================== resetPassword ========================

    @Nested
    @DisplayName("重置密码 - resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("应当成功重置密码并返回新 JWT Token")
        void shouldResetPasswordSuccessfully() {
            // Given
            String redisKey = "verification_code:" + EMAIL;
            when(redisUtil.getVerificationCode(redisKey)).thenReturn(VERIFICATION_CODE);
            when(userRepository.getUserIdByEmail(EMAIL)).thenReturn(USER_ID);
            when(userRepository.getRoleByUserId(USER_ID)).thenReturn("0");
            passwordUtilsMock.when(() -> PasswordUtils.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(jwtUtils.generateToken(any(LoginUser.class))).thenReturn(TOKEN);

            // When
            String result = authService.resetPassword(EMAIL, VERIFICATION_CODE, PASSWORD);

            // Then
            assertNotNull(result);
            assertEquals(TOKEN, result);
            verify(redisUtil).getVerificationCode(redisKey);
            verify(userRepository).getUserIdByEmail(EMAIL);
            verify(userRepository).updatePasswordByEmail(EMAIL, ENCODED_PASSWORD);
            verify(redisUtil).removeVerificationCode(redisKey);
            verify(jwtUtils).generateToken(any(LoginUser.class));
        }

        @Test
        @DisplayName("邮箱为空时应当抛出 EMAIL_EMPTY 异常")
        void shouldThrowExceptionWhenEmailEmpty() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.resetPassword(null, VERIFICATION_CODE, PASSWORD));
            assertEquals(UserErrorCode.EMAIL_EMPTY.getCode(), exception.getCode());

            exception = assertThrows(BusinessException.class,
                    () -> authService.resetPassword("", VERIFICATION_CODE, PASSWORD));
            assertEquals(UserErrorCode.EMAIL_EMPTY.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("验证码为空时应当抛出 VERIFICATION_CODE_EMPTY 异常")
        void shouldThrowExceptionWhenCodeEmpty() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.resetPassword(EMAIL, null, PASSWORD));
            assertEquals(UserErrorCode.VERIFICATION_CODE_EMPTY.getCode(), exception.getCode());

            exception = assertThrows(BusinessException.class,
                    () -> authService.resetPassword(EMAIL, "", PASSWORD));
            assertEquals(UserErrorCode.VERIFICATION_CODE_EMPTY.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("密码为空时应当抛出 PASSWORD_EMPTY 异常")
        void shouldThrowExceptionWhenPasswordEmpty() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.resetPassword(EMAIL, VERIFICATION_CODE, null));
            assertEquals(UserErrorCode.PASSWORD_EMPTY.getCode(), exception.getCode());

            exception = assertThrows(BusinessException.class,
                    () -> authService.resetPassword(EMAIL, VERIFICATION_CODE, ""));
            assertEquals(UserErrorCode.PASSWORD_EMPTY.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("验证码过期时应当抛出 VERIFICATION_CODE_EXPIRED 异常")
        void shouldThrowExceptionWhenCodeExpired() {
            // Given
            String redisKey = "verification_code:" + EMAIL;
            when(redisUtil.getVerificationCode(redisKey)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.resetPassword(EMAIL, VERIFICATION_CODE, PASSWORD));
            assertEquals(UserErrorCode.VERIFICATION_CODE_EXPIRED.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("验证码错误时应当抛出 VERIFICATION_CODE_ERROR 异常")
        void shouldThrowExceptionWhenCodeError() {
            // Given
            String redisKey = "verification_code:" + EMAIL;
            when(redisUtil.getVerificationCode(redisKey)).thenReturn("999999");

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.resetPassword(EMAIL, VERIFICATION_CODE, PASSWORD));
            assertEquals(UserErrorCode.VERIFICATION_CODE_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("用户不存在时应当抛出 USER_NOT_EXIST_BY_EMAIL 异常")
        void shouldThrowExceptionWhenUserNotExistByEmail() {
            // Given
            String redisKey = "verification_code:" + EMAIL;
            when(redisUtil.getVerificationCode(redisKey)).thenReturn(VERIFICATION_CODE);
            when(userRepository.getUserIdByEmail(EMAIL)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.resetPassword(EMAIL, VERIFICATION_CODE, PASSWORD));
            assertEquals(UserErrorCode.USER_NOT_EXIST_BY_EMAIL.getCode(), exception.getCode());
        }
    }

    // ======================== register ========================

    @Nested
    @DisplayName("注册 - register")
    class RegisterTests {

        @Test
        @DisplayName("应当成功注册并返回用户 ID")
        void shouldRegisterSuccessfully() {
            // Given
            UserRegisterVO request = buildRegisterRequest();
            String redisKey = "verification_code:" + EMAIL;

            when(userRepository.getUserIdByEmail(EMAIL)).thenReturn(null);
            when(redisUtil.getVerificationCode(redisKey)).thenReturn(VERIFICATION_CODE);
            passwordUtilsMock.when(() -> PasswordUtils.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
            User savedUser = buildUser(USER_ID);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // When
            Long result = authService.register(request);

            // Then
            assertNotNull(result);
            assertEquals(USER_ID, result);
            verify(redisUtil).removeVerificationCode(redisKey);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("邮箱已存在时应当抛出 USER_ALREADY_EXISTS 异常")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // Given
            UserRegisterVO request = buildRegisterRequest();
            when(userRepository.getUserIdByEmail(EMAIL)).thenReturn(USER_ID);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.register(request));
            assertEquals(UserErrorCode.USER_ALREADY_EXISTS.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("验证码过期时应当抛出 VERIFICATION_CODE_EXPIRED 异常")
        void shouldThrowExceptionWhenRegisterCodeExpired() {
            // Given
            UserRegisterVO request = buildRegisterRequest();
            String redisKey = "verification_code:" + EMAIL;

            when(userRepository.getUserIdByEmail(EMAIL)).thenReturn(null);
            when(redisUtil.getVerificationCode(redisKey)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.register(request));
            assertEquals(UserErrorCode.VERIFICATION_CODE_EXPIRED.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("验证码错误时应当抛出 VERIFICATION_CODE_ERROR 异常")
        void shouldThrowExceptionWhenRegisterCodeError() {
            // Given
            UserRegisterVO request = buildRegisterRequest();
            String redisKey = "verification_code:" + EMAIL;

            when(userRepository.getUserIdByEmail(EMAIL)).thenReturn(null);
            when(redisUtil.getVerificationCode(redisKey)).thenReturn("999999");

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.register(request));
            assertEquals(UserErrorCode.VERIFICATION_CODE_ERROR.getCode(), exception.getCode());
        }
    }

    // ======================== 辅助方法 ========================

    private User buildUser(Long id) {
        return User.builder()
                .id(id)
                .name("测试用户")
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .grade("大一")
                .sex("男")
                .age(18)
                .role(0)
                .build();
    }

    private UserRegisterVO buildRegisterRequest() {
        UserRegisterVO vo = new UserRegisterVO();
        vo.setName("测试用户");
        vo.setEmail(EMAIL);
        vo.setCode(VERIFICATION_CODE);
        vo.setPassword(PASSWORD);
        vo.setGrade("大一");
        vo.setSex("男");
        vo.setAge(18);
        return vo;
    }
}
