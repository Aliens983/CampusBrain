package com.laoliu.cas.system.interfaces.controller.app;

import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.UserErrorCode;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.redis.util.RedisUtil;
import com.laoliu.cas.system.application.service.CaptchaService;
import com.laoliu.cas.system.interfaces.dto.response.CaptchaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码接口（扁平路径，供前端直接调用）
 * <p>
 * {uuid} 与图片均存 Redis（内存态）：GET /captcha 生成并返回图片取回地址，
 * GET /captcha/image/{uuid} 动态返回 PNG 字节；生成接口带 IP 级简单频控。
 *
 * @author forever-king
 */
@Tag(name = "图形验证码（用户）")
@RestController
@RequestMapping("/captcha")
@RequiredArgsConstructor
public class GraphicController {

    private final CaptchaService captchaService;
    private final RedisUtil redisUtil;

    /** 验证码图片 base64 在 Redis 中的 key 前缀（与 CaptchaServiceImpl 写入侧保持一致） */
    private static final String CAPTCHA_IMG_KEY_PREFIX = "captcha:img:";

    /** 生成接口频控：同一 IP 在窗口内最多允许的次数 */
    private static final int CAPTCHA_RATE_LIMIT_MAX = 30;

    /** 生成接口频控窗口（秒） */
    private static final int CAPTCHA_RATE_LIMIT_WINDOW_SECONDS = 60;

    /** 生成接口频控 Redis key 前缀 */
    private static final String CAPTCHA_RATE_KEY_PREFIX = "captcha:rate:";

    @Operation(summary = "获取图形验证码", description = "返回uuid和验证码图片取回URL，验证码5分钟内有效，同一IP 60秒内限30次")
    @GetMapping
    public CommonResult<CaptchaResponse> getGraphicCaptcha(HttpServletRequest request) {
        checkRateLimit(resolveClientIp(request));
        // CaptchaService 已直接返回 CaptchaResponse，无需在 Controller 二次拼装（7.3.3）
        return CommonResult.success(captchaService.generateCaptcha());
    }

    @Operation(summary = "获取验证码图片", description = "按 uuid 从 Redis 取回验证码 PNG，取后即过期，前端无需缓存")
    @GetMapping("/image/{uuid}")
    public ResponseEntity<byte[]> getCaptchaImage(@PathVariable String uuid) {
        String base64 = redisUtil.getVerificationCode(CAPTCHA_IMG_KEY_PREFIX + uuid);
        if (base64 == null || base64.isEmpty()) {
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_EXPIRED);
        }
        byte[] imageBytes = Base64.getDecoder().decode(base64);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore())
                .body(imageBytes);
    }

    /** 接口级简单计数限流；阈值/窗口为固定常量，避免单 IP 刷验证码消耗 Redis 与图片生成算力 */
    private void checkRateLimit(String ip) {
        String key = CAPTCHA_RATE_KEY_PREFIX + ip;
        long count = redisUtil.increment(key);
        if (count == 1) {
            redisUtil.expire(key, CAPTCHA_RATE_LIMIT_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        if (count > CAPTCHA_RATE_LIMIT_MAX) {
            throw new BusinessException(UserErrorCode.EMAIL_SEND_TOO_FREQUENTLY);
        }
    }

    /** 取真实客户端 IP：优先取 X-Forwarded-For（网关/代理场景），否则回退 remoteAddr */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
