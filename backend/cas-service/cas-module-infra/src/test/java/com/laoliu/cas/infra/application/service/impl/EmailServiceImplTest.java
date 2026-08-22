package com.laoliu.cas.infra.application.service.impl;

import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.CommonErrorCode;
import com.laoliu.cas.infra.application.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * EmailServiceImpl 单元测试。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("邮件服务单元测试")
class EmailServiceImplTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private Environment environment;

    private EmailService emailService;

    private static final String TO = "recipient@example.com";
    private static final String SUBJECT = "测试主题";
    private static final String CONTENT = "测试内容";
    private static final String FROM = "sender@163.com";
    private static final String SPRING_MAIL_USERNAME = "spring.mail.username";

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(javaMailSender, environment);
    }

    @Nested
    @DisplayName("发送邮件 - sendEmail")
    class SendEmailTests {

        @Test
        @DisplayName("应当成功发送邮件")
        void shouldSendEmailSuccessfully() {
            // Given
            when(environment.getProperty(SPRING_MAIL_USERNAME)).thenReturn(FROM);
            doNothing().when(javaMailSender).send(any(SimpleMailMessage.class));

            // When
            emailService.sendEmail(TO, SUBJECT, CONTENT);

            // Then
            verify(environment).getProperty(SPRING_MAIL_USERNAME);
            verify(javaMailSender).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("邮件发送失败时应当抛出 EMAIL_SEND_FAILED 异常")
        void shouldThrowExceptionWhenSendFails() {
            // Given
            when(environment.getProperty(SPRING_MAIL_USERNAME)).thenReturn(FROM);
            doThrow(new RuntimeException("SMTP connection failed"))
                    .when(javaMailSender).send(any(SimpleMailMessage.class));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> emailService.sendEmail(TO, SUBJECT, CONTENT));
            assertEquals(CommonErrorCode.EMAIL_SEND_FAILED.getCode(), exception.getCode());
            verify(javaMailSender).send(any(SimpleMailMessage.class));
        }
    }
}
