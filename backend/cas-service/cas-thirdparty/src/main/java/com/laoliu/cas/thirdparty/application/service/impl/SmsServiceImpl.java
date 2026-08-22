package com.laoliu.cas.thirdparty.application.service.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.CommonErrorCode;
import com.laoliu.cas.thirdparty.application.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author forever-king
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final Client dysmsClient;

    @Value("${aliyun.sms.sign-name:校园预约系统}")
    private String signName;

    @Override
    public void sendSms(String phoneNumber, String templateCode, String templateParam) {
        SendSmsRequest request = new SendSmsRequest()
                .setPhoneNumbers(phoneNumber)
                .setSignName(signName)
                .setTemplateCode(templateCode)
                .setTemplateParam(templateParam);

        try {
            SendSmsResponse response = dysmsClient.sendSms(request);
            log.info("SMS sent successfully: {}", response.getBody());
        } catch (Exception e) {
            log.error("SMS send error: {}", e.getMessage());
            throw new BusinessException(CommonErrorCode.SMS_SEND_FAILED);
        }
    }
}
