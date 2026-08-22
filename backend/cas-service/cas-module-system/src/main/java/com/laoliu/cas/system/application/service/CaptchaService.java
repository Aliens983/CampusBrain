package com.laoliu.cas.system.application.service;

import com.laoliu.cas.system.application.service.vo.CaptchaResult;

/**
 * @author forever-king
 */
public interface CaptchaService {

    /** 生成图形验证码 */
    CaptchaResult generateCaptcha();
}
