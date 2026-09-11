package com.atmoto.recruit.biz.mail;

/**
 * 邮件发送器接口
 * <p>开发环境使用 MockEmailSender 日志打印验证码（mail.provider=mock，默认），
 * 生产环境走 SMTP 真实发送（mail.provider=smtp）。</p>
 */
public interface EmailSender {

    /**
     * 发送验证码邮件
     *
     * @param toEmail 收件邮箱
     * @param code    6位验证码
     * @return true 发送成功，false 发送失败
     */
    boolean sendCodeEmail(String toEmail, String code);
}
