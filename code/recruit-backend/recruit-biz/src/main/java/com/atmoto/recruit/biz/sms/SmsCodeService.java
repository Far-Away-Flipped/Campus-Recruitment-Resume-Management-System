package com.atmoto.recruit.biz.sms;

/**
 * 短信验证码服务接口
 */
public interface SmsCodeService {

    /**
     * 发送短信验证码
     * <p>包含六层防刷：手机号60秒限制、IP 60秒3次、日上限10条、图形验证码前置校验</p>
     *
     * @param phone 手机号
     * @return 生成的验证码（开发环境通过日志输出，生产环境通过短信网关发送）
     */
    String sendCode(String phone);

    /**
     * 校验短信验证码并消费（一次性验证）
     *
     * @param phone 手机号
     * @param code  验证码
     * @return true 验证通过，false 验证失败或验证码不存在
     */
    boolean verifyCode(String phone, String code);

    /**
     * 校验图形验证码
     * <p>调用前应先通过 captchaCode 校验，本方法由 Controller 层在调用 sendCode 之前调用</p>
     *
     * @param captchaKey  图形验证码缓存key
     * @param captchaCode 用户输入的图形验证码
     */
    void verifyCaptcha(String captchaKey, String captchaCode);
}
