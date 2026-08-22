package com.laoliu.cas.infra.application.service;

/**
 * 邮件发送应用层服务接口。
 *
 * @author forever-king
 */
public interface EmailService {
    /** 发送邮件 */
    void sendEmail(String to, String subject, String content);
}
