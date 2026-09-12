package com.laoliu.cas.system.application.service;

import com.laoliu.cas.system.application.service.vo.CaptchaResult;

/**
 * @author forever-king
 */
public interface CaptchaService {

    /** 生成图形验证码 */
    CaptchaResult generateCaptcha();

    /**
     * 校验图形验证码。
     * <p>
     * 一次性语义：无论校验成功与否，只要取出就立即删除，
     * 避免同一张验证码被反复提交试错（防暴力破解）。
     *
     * @param uuid 验证码标识，由 {@link #generateCaptcha()} 返回
     * @param code 用户输入的算术结果
     * @throws com.laoliu.cas.common.exception.BusinessException 验证码为空 / 已过期 / 不正确
     */
    void validateCaptcha(String uuid, String code);
}
