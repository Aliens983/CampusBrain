package com.laoliu.cas.system.interfaces.controller.app;

import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.redis.util.RedisUtil;
import com.laoliu.cas.system.application.service.CaptchaService;
import com.laoliu.cas.system.interfaces.dto.response.CaptchaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GraphicController 客户端 IP 解析测试.
 * <p>
 * 背景：验证码生成频控此前取 X-Forwarded-For 首段，而 nginx 以
 * $proxy_add_x_forwarded_for 追加式透传，客户端自带值排在首位——频控可被
 * 轮换伪造头无限绕过。现改为信任 nginx 覆盖式设置的 X-Real-IP.
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("图形验证码控制器 IP 解析")
class GraphicControllerTest {

    @Mock
    private CaptchaService captchaService;

    @Mock
    private RedisUtil redisUtil;

    private GraphicController controller;

    private void newController() {
        controller = new GraphicController(captchaService, redisUtil);
    }

    @Test
    @DisplayName("有 X-Real-IP 时按其限频（nginx 覆盖式设置，不可伪造）")
    void shouldRateLimitByRealIpHeader() {
        newController();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "10.1.2.3");
        // 攻击者自带的 XFF 首段应被忽略
        request.addHeader("X-Forwarded-For", "1.2.3.4, 10.1.2.3");
        when(redisUtil.increment(anyString())).thenReturn(1L);
        when(captchaService.generateCaptcha()).thenReturn(new CaptchaResponse());

        CommonResult<CaptchaResponse> result = controller.getGraphicCaptcha(request);

        verify(redisUtil).increment("captcha:rate:10.1.2.3");
        org.junit.jupiter.api.Assertions.assertNotNull(result);
    }

    @Test
    @DisplayName("无 X-Real-IP 时回退 remoteAddr，且绝不相信 XFF 首段")
    void shouldFallbackToRemoteAddrIgnoringForwardedHeader() {
        newController();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.0.9");
        request.addHeader("X-Forwarded-For", "1.2.3.4");
        when(redisUtil.increment(anyString())).thenReturn(1L);
        when(captchaService.generateCaptcha()).thenReturn(new CaptchaResponse());

        controller.getGraphicCaptcha(request);

        verify(redisUtil).increment("captcha:rate:192.168.0.9");
    }
}
