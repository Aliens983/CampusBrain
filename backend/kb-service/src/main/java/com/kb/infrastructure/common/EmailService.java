package com.kb.infrastructure.common;

/**
 * 邮件发送服务接口
 *
 * @author forever-king
 */
public interface EmailService {

    /**
     * 发送邮件
     *
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件正文
     */
    void sendEmail(String to, String subject, String content);
}
