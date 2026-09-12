package com.laoliu.cas.system.application.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ShearCaptcha;
import cn.hutool.captcha.generator.MathGenerator;
import cn.hutool.core.math.Calculator;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.UserErrorCode;
import com.laoliu.cas.infra.application.service.FileService;
import com.laoliu.cas.redis.util.RedisUtil;
import com.laoliu.cas.system.application.service.CaptchaService;
import com.laoliu.cas.system.application.service.vo.CaptchaResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * @author forever-king
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private final RedisUtil redisUtil;
    private final FileService fileService;

    @Value("${file.upload.server-address:http://localhost:18080}")
    private String serverAddress;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /** 图形验证码在 Redis 中的 key 前缀，生成与校验两处必须保持一致 */
    private static final String CAPTCHA_KEY_PREFIX = "captcha:";

    @Override
    public CaptchaResult generateCaptcha() {
        String uuid = UUID.randomUUID().toString();
        String redisKey = CAPTCHA_KEY_PREFIX + uuid;

        ShearCaptcha captcha = CaptchaUtil.createShearCaptcha(130, 38, 4, 4);
        captcha.setGenerator(new MathGenerator());
        captcha.createCode();

        String code = captcha.getCode();
        String expr = code.replace("=", "").trim();
        double calcResult = Calculator.conversion(expr);
        String end = String.valueOf((int) calcResult);

        redisUtil.setVerificationCode(redisKey, end, 300);

        try {
            File tempFile = File.createTempFile("captcha-", ".png");
            captcha.write(tempFile);
            String fileUrl = fileService.uploadFile(tempFile, "captcha");
            tempFile.delete();

            String imageUrl = serverAddress + contextPath + fileUrl;

            return CaptchaResult.builder()
                    .uuid(uuid)
                    .imageUrl(imageUrl)
                    .build();
        } catch (IOException e) {
            log.error("生成验证码图片失败", e);
            throw new RuntimeException("生成验证码图片失败", e);
        }
    }

    @Override
    public void validateCaptcha(String uuid, String code) {
        if (uuid == null || uuid.isEmpty() || code == null || code.isEmpty()) {
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_EMPTY);
        }

        String redisKey = CAPTCHA_KEY_PREFIX + uuid;
        String storedCode = redisUtil.getVerificationCode(redisKey);
        if (storedCode == null) {
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        // 一次性：取出即失效，同一张验证码不能用于第二次登录尝试
        redisUtil.removeVerificationCode(redisKey);

        if (!storedCode.equals(code.trim())) {
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_ERROR);
        }
    }
}
