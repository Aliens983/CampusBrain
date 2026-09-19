package com.laoliu.cas.infra.api.email;

/**
 * 邮件发送跨模块服务接口（2.3：契约独立至 cas-module-infra-api）
 *
 * @author forever-king
 */
public interface EmailService {
    /** 发送邮件 */
    void sendEmail(String to, String subject, String content);
}
