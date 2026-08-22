package com.laoliu.cas.system.application.service.impl;

import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.UserErrorCode;
import com.laoliu.cas.common.util.CodeGenerator;
import com.laoliu.cas.infra.application.service.EmailService;
import com.laoliu.cas.redis.util.RedisUtil;
import com.laoliu.cas.system.application.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author forever-king
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final EmailService emailService;
    private final RedisUtil redisUtil;

    @Override
    public void sendVerificationCode(String email) {
        if (email == null || email.isEmpty()) {
            throw new BusinessException(UserErrorCode.EMAIL_NOT_PROVIDED);
        }

        String rateLimitKey = "rate_limit:email:" + email;
        String lastSendTime = redisUtil.getVerificationCode(rateLimitKey);
        if (lastSendTime != null) {
            throw new BusinessException(UserErrorCode.EMAIL_SEND_TOO_FREQUENTLY);
        }

        String code = CodeGenerator.generateCode();
        String redisKey = "verification_code:" + email;
        redisUtil.setVerificationCode(redisKey, code, 300);
        redisUtil.setVerificationCode(rateLimitKey, "1", 60);

        String subject = "校园预约系统 - 邮箱验证码";
        String content = "您的验证码是：" + code + "，5 分钟内有效，请勿泄露给他人。";
        emailService.sendEmail(email, subject, content);
    }
}
