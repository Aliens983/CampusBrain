package com.laoliu.cas.system.application.service;

/**
 * @author forever-king
 */
public interface EmailVerificationService {

    /** 发送邮箱验证码 */
    void sendVerificationCode(String email);
}
