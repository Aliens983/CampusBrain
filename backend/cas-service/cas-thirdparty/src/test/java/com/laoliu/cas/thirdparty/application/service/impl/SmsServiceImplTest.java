package com.laoliu.cas.thirdparty.application.service.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.CommonErrorCode;
import com.laoliu.cas.thirdparty.application.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SmsServiceImpl 单元测试。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("短信服务单元测试")
class SmsServiceImplTest {

    @Mock
    private Client dysmsClient;

    private SmsServiceImpl smsService;

    private static final String PHONE = "13800138000";
    private static final String TEMPLATE_CODE = "SMS_123456789";
    private static final String TEMPLATE_PARAM = "{\"code\":\"123456\"}";
    private static final String SIGN_NAME = "校园预约系统";

    @BeforeEach
    void setUp() {
        smsService = new SmsServiceImpl(dysmsClient);
        ReflectionTestUtils.setField(smsService, "signName", SIGN_NAME);
    }

    @Nested
    @DisplayName("发送短信 - sendSms")
    class SendSmsTests {

        @Test
        @DisplayName("应当成功发送短信")
        void shouldSendSmsSuccessfully() throws Exception {
            // Given
            SendSmsResponse mockResponse = buildSendSmsResponse();
            when(dysmsClient.sendSms(any(SendSmsRequest.class))).thenReturn(mockResponse);

            // When
            smsService.sendSms(PHONE, TEMPLATE_CODE, TEMPLATE_PARAM);

            // Then
            verify(dysmsClient).sendSms(any(SendSmsRequest.class));
        }

        @Test
        @DisplayName("发送失败时应当抛出 SMS_SEND_FAILED 异常")
        void shouldThrowExceptionWhenSendFails() throws Exception {
            // Given
            when(dysmsClient.sendSms(any(SendSmsRequest.class)))
                    .thenThrow(new RuntimeException("SMS service unavailable"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> smsService.sendSms(PHONE, TEMPLATE_CODE, TEMPLATE_PARAM));
            assertEquals(CommonErrorCode.SMS_SEND_FAILED.getCode(), exception.getCode());
        }
    }

    // ======================== 辅助方法 ========================

    private SendSmsResponse buildSendSmsResponse() {
        SendSmsResponse response = new SendSmsResponse();
        SendSmsResponseBody body = new SendSmsResponseBody();
        body.setCode("OK");
        body.setMessage("OK");
        body.setBizId("biz-id-123");
        response.setBody(body);
        return response;
    }
}
