package com.laoliu.cas.infra.application.service.impl;

import com.laoliu.cas.infra.application.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @author forever-king
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final Environment environment;

    @Override
    @Async
    public void sendEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            String fromEmail = environment.getProperty("spring.mail.username");
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            javaMailSender.send(message);
            log.info("邮件发送成功，发件人：{}，收件人：{}，主题：{}，内容：{}", fromEmail, to, subject, content);
        } catch (Exception e) {
            // 1.5.1：本方法为 @Async 异步执行，调用方不会也无法捕获这里抛出的异常，
            // 再抛 BusinessException 只会落到 AsyncUncaughtExceptionHandler，对业务毫无意义。
            // 邮件属于"尽力通知"，失败仅记录日志，不影响主下单/审核事务。
            log.error("邮件发送失败，收件人：{}，主题：{}，错误信息：{}", to, subject, e.getMessage(), e);
        }
    }
}
