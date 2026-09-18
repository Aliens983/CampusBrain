package com.laoliu.cas.system.application.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ShearCaptcha;
import cn.hutool.captcha.generator.MathGenerator;
import cn.hutool.core.math.Calculator;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.UserErrorCode;
import com.laoliu.cas.redis.util.RedisUtil;
import com.laoliu.cas.system.application.service.CaptchaService;
import com.laoliu.cas.system.interfaces.dto.response.CaptchaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 图形验证码服务
 * <p>
 * 答案与图片均存 Redis（内存态），不再落盘到 uploads/captcha，避免磁盘持续堆积；
 * 图片通过 GET /captcha/image/{uuid} 动态取回，前端无需感知变化。
 *
 * @author forever-king
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private final RedisUtil redisUtil;

    @Value("${file.upload.server-address:http://localhost:18080}")
    private String serverAddress;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /** 图形验证码在 Redis 中的 key 前缀，生成与校验两处必须保持一致 */
    private static final String CAPTCHA_KEY_PREFIX = "captcha:";

    /** 验证码图片 base64 在 Redis 中的 key 前缀（与 GraphicController 的读取侧保持一致） */
    private static final String CAPTCHA_IMG_KEY_PREFIX = "captcha:img:";

    /** 验证码有效时长（秒）：答案与图片一致，短 TTL 内存态 */
    private static final long CAPTCHA_TTL_SECONDS = 300;

    /** 图片取回路径（与 GraphicController 的路由保持一致） */
    private static final String CAPTCHA_IMAGE_PATH = "/captcha/image/";

    @Override
    public CaptchaResponse generateCaptcha() {
        String uuid = UUID.randomUUID().toString();
        String redisKey = CAPTCHA_KEY_PREFIX + uuid;

        ShearCaptcha captcha = CaptchaUtil.createShearCaptcha(130, 38, 4, 4);
        captcha.setGenerator(new MathGenerator());
        captcha.createCode();

        String code = captcha.getCode();
        String expr = code.replace("=", "").trim();
        double calcResult = Calculator.conversion(expr);
        String end = String.valueOf((int) calcResult);

        redisUtil.setVerificationCode(redisKey, end, CAPTCHA_TTL_SECONDS);

        // 图片转 base64 存 Redis（不再经 FileService 落盘），TTL 与答案保持一致
        String dataUri = captcha.getImageBase64();
        String pureBase64 = dataUri.substring(dataUri.indexOf(',') + 1);
        redisUtil.setVerificationCode(CAPTCHA_IMG_KEY_PREFIX + uuid, pureBase64, CAPTCHA_TTL_SECONDS);

        // 返回绝对 URL，前端 new URL(...) 解析路径的逻辑无需改动
        String imageUrl = serverAddress + contextPath + CAPTCHA_IMAGE_PATH + uuid;

        return CaptchaResponse.builder()
                .uuid(uuid)
                .imageUrl(imageUrl)
                .build();
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
