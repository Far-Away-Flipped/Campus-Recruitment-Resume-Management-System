package com.atmoto.recruit.biz.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 模拟邮件发送器（开发环境，mail.provider=mock 或未配置时生效）
 * <p>通过日志打印验证码，不实际调用 SMTP。开发调试时从后端日志读取。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "mail", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockEmailSender implements EmailSender {

    @Override
    public boolean sendCodeEmail(String toEmail, String code) {
        log.info("【模拟邮件】发送至 {}：验证码 {}", toEmail, code);
        return true;
    }
}
