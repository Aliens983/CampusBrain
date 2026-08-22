package com.laoliu.cas.system.application.service.impl;

import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.UserErrorCode;
import com.laoliu.cas.common.util.CodeGenerator;
import com.laoliu.cas.infra.application.service.EmailService;
import com.laoliu.cas.redis.util.RedisUtil;
import com.laoliu.cas.system.application.service.EmailVerificationService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EmailVerificationServiceImpl 单元测试。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("邮箱验证码服务单元测试")
class EmailVerificationServiceImplTest {

    @Mock
    private EmailService emailService;

    @Mock
    private RedisUtil redisUtil;

    private MockedStatic<CodeGenerator> codeGeneratorMock;

    private EmailVerificationService emailVerificationService;

    private static final String EMAIL = "test@example.com";
    private static final String CODE = "123456";
    private static final String RATE_LIMIT_KEY = "rate_limit:email:" + EMAIL;
    private static final String REDIS_KEY = "verification_code:" + EMAIL;
    private static final String SUBJECT = "校园预约系统 - 邮箱验证码";

    @BeforeEach
    void setUp() {
        codeGeneratorMock = mockStatic(CodeGenerator.class);
        emailVerificationService = new EmailVerificationServiceImpl(emailService, redisUtil);
    }

    @AfterEach
    void tearDown() {
        codeGeneratorMock.close();
    }

    @Nested
    @DisplayName("发送验证码 - sendVerificationCode")
    class SendVerificationCodeTests {

        @Test
        @DisplayName("应当成功发送验证码")
        void shouldSendVerificationCodeSuccessfully() {
            // Given
            when(redisUtil.getVerificationCode(RATE_LIMIT_KEY)).thenReturn(null);
            codeGeneratorMock.when(CodeGenerator::generateCode).thenReturn(CODE);

            // When
            emailVerificationService.sendVerificationCode(EMAIL);

            // Then
            verify(redisUtil).setVerificationCode(REDIS_KEY, CODE, 300);
            verify(redisUtil).setVerificationCode(RATE_LIMIT_KEY, "1", 60);
            verify(emailService).sendEmail(eq(EMAIL), eq(SUBJECT), contains(CODE));
        }

        @Test
        @DisplayName("邮箱为空时应当抛出 EMAIL_NOT_PROVIDED 异常")
        void shouldThrowExceptionWhenEmailEmpty() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> emailVerificationService.sendVerificationCode(null));
            assertEquals(UserErrorCode.EMAIL_NOT_PROVIDED.getCode(), exception.getCode());

            exception = assertThrows(BusinessException.class,
                    () -> emailVerificationService.sendVerificationCode(""));
            assertEquals(UserErrorCode.EMAIL_NOT_PROVIDED.getCode(), exception.getCode());

            verify(redisUtil, never()).setVerificationCode(anyString(), anyString(), anyLong());
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("发送频率限制时应当抛出 EMAIL_SEND_TOO_FREQUENTLY 异常")
        void shouldThrowExceptionWhenTooFrequent() {
            // Given
            when(redisUtil.getVerificationCode(RATE_LIMIT_KEY)).thenReturn("1");

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> emailVerificationService.sendVerificationCode(EMAIL));
            assertEquals(UserErrorCode.EMAIL_SEND_TOO_FREQUENTLY.getCode(), exception.getCode());

            verify(redisUtil, never()).setVerificationCode(anyString(), anyString(), anyLong());
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        }
    }
}
