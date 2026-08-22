package com.kb.infrastructure.security;

/**
 * 邮箱验证码服务接口
 *
 * @author forever-king
 */
public interface EmailVerificationService {

    /**
     * 发送邮箱验证码
     *
     * @param email 收件人邮箱
     */
    void sendVerificationCode(String email);

    /**
     * 校验验证码
     *
     * @param email 邮箱
     * @param code 用户提交的验证码
     * @return true=验证通过
     */
    boolean verifyCode(String email, String code);
}
