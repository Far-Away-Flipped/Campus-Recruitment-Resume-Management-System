package com.atmoto.recruit.biz.sms;

/**
 * 短信发送器接口
 * <p>开发环境使用 MockSmsSender 日志打印验证码，生产环境对接阿里云短信服务</p>
 */
public interface SmsSender {

    /**
     * 发送短信
     *
     * @param phone   手机号
     * @param content 短信内容
     * @return true 发送成功，false 发送失败
     */
    boolean send(String phone, String content);
}
