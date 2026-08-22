package com.kb.infrastructure.security;

import com.kb.infrastructure.common.CodeGenerator;
import com.kb.infrastructure.common.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码服务实现
 * <p>
 * 验证码存储在 Redis，Key 格式：verification_code:{email}，TTL 5 分钟。
 * 60 秒内同一邮箱只能发送一次（rate_limit:email:{email}）。
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final EmailService emailService;
    private final StringRedisTemplate stringRedisTemplate;

    /** 验证码 Redis Key 前缀 */
    private static final String CODE_PREFIX = "verification_code:";

    /** 频率限制 Redis Key 前缀 */
    private static final String RATE_LIMIT_PREFIX = "rate_limit:email:";

    /** 验证码有效期（秒） */
    private static final int CODE_TTL = 300;

    /** 发送频率限制（秒） */
    private static final int RATE_LIMIT_TTL = 60;

    @Override
    public void sendVerificationCode(String email) {
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("邮箱不能为空");
        }

        String rateLimitKey = RATE_LIMIT_PREFIX + email;
        String lastSendTime = stringRedisTemplate.opsForValue().get(rateLimitKey);
        if (lastSendTime != null) {
            throw new RuntimeException("验证码发送过于频繁，请 60 秒后再试");
        }

        String code = CodeGenerator.generateCode();
        String codeKey = CODE_PREFIX + email;
        stringRedisTemplate.opsForValue().set(codeKey, code, CODE_TTL, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(rateLimitKey, "1", RATE_LIMIT_TTL, TimeUnit.SECONDS);

        String subject = "知识库平台 - 邮箱验证码";
        String content = "您的验证码是：" + code + "，5 分钟内有效，请勿泄露给他人。";
        emailService.sendEmail(email, subject, content);
        log.info("验证码已发送至 {}，Redis Key={}", email, codeKey);
    }

    @Override
    public boolean verifyCode(String email, String code) {
        if (email == null || code == null) {
            return false;
        }
        String storedCode = stringRedisTemplate.opsForValue().get(CODE_PREFIX + email);
        if (storedCode == null) {
            return false;
        }
        if (!storedCode.equals(code)) {
            return false;
        }
        stringRedisTemplate.delete(CODE_PREFIX + email);
        return true;
    }
}
