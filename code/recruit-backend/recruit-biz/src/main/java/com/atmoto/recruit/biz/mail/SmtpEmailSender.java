package com.atmoto.recruit.biz.mail;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP 邮件发送器（生产环境，mail.provider=smtp 激活）
 * <p>走 Spring Boot 标准 spring.mail.* 配置（个人/企业邮箱 SMTP 均可，
 * 如 QQ 邮箱 smtp.qq.com:465 + 授权码）。</p>
 * <p>fail-closed：host/username/password 任一缺失时拒绝启动，
 * 绝不带空凭证运行到发送时才失败。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mail", name = "provider", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String host;

    @Value("${spring.mail.username:}")
    private String from;

    @Value("${spring.mail.password:}")
    private String password;

    @PostConstruct
    void checkConfig() {
        if (isBlank(host) || isBlank(from) || isBlank(password)) {
            throw new IllegalStateException(
                    "mail.provider=smtp 但 spring.mail.host/username/password 未配置，拒绝启动");
        }
        log.info("SMTP 邮件发送器已初始化：host={}, from={}", host, from);
    }

    @Override
    public boolean sendCodeEmail(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject("【遨天科技校园招聘】验证码");
            message.setText("您的验证码是：" + code + "，有效期5分钟，请勿泄露。如非本人操作请忽略本邮件。");
            mailSender.send(message);
            log.info("验证码邮件发送成功：to={}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("验证码邮件发送失败：to={}, error={}", toEmail, e.getMessage());
            return false;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
