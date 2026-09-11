package com.atmoto.recruit.biz.mail;

/**
 * 邮箱验证码服务接口
 * <p>结构与 SmsCodeService 对称：防刷 + 生成 + 缓存 + 一次性消费。
 * 验证码缓存 key 为邮箱地址（注册与重置均按邮箱验码）。</p>
 */
public interface EmailCodeService {

    /**
     * 发送邮箱验证码
     * <p>防刷：同邮箱60秒1次、同IP 60秒3次、同邮箱日上限10条（图形验证码前置校验由调用方完成）。
     * 验证码不外传——mock 走日志、真实邮件走 SMTP。</p>
     *
     * @param email 收件邮箱
     */
    void sendCode(String email);

    /**
     * 校验邮箱验证码并消费（一次性验证）
     *
     * @param email 邮箱
     * @param code  验证码
     * @return true 验证通过，false 验证失败或验证码不存在
     */
    boolean verifyCode(String email, String code);
}
