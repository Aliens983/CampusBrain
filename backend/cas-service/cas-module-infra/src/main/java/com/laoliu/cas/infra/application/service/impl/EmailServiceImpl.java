package com.laoliu.cas.infra.application.service.impl;

import com.laoliu.cas.infra.application.service.EmailService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务。
 * <p>
 * 业务指标（5.2.2）：{@code email_sent_total}（tag result=success/failed），
 * 用于统计通知邮件成功率。
 *
 * @author forever-king
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final Environment environment;
    private final Counter sentCounter;
    private final Counter failedCounter;

    public EmailServiceImpl(JavaMailSender javaMailSender, Environment environment,
                            MeterRegistry meterRegistry) {
        this.javaMailSender = javaMailSender;
        this.environment = environment;
        this.sentCounter = Counter.builder("email.sent")
                .description("Email send attempts")
                .tag("result", "success")
                .register(meterRegistry);
        this.failedCounter = Counter.builder("email.sent")
                .description("Email send attempts")
                .tag("result", "failed")
                .register(meterRegistry);
    }

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
            sentCounter.increment();
            log.info("邮件发送成功，发件人：{}，收件人：{}，主题：{}，内容：{}", fromEmail, to, subject, content);
        } catch (Exception e) {
            failedCounter.increment();
            // 1.5.1：本方法为 @Async 异步执行，调用方不会也无法捕获这里抛出的异常，
            // 再抛 BusinessException 只会落到 AsyncUncaughtExceptionHandler，对业务毫无意义。
            // 邮件属于"尽力通知"，失败仅记录日志，不影响主下单/审核事务。
            log.error("邮件发送失败，收件人：{}，主题：{}，错误信息：{}", to, subject, e.getMessage(), e);
        }
    }
}
