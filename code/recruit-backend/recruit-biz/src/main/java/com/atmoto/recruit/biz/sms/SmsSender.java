package com.atmoto.recruit.biz.sms;

import java.util.Map;

/**
 * 短信发送器接口
 * <p>开发环境使用 MockSmsSender 日志打印验证码（sms.provider=mock，默认），
 * 生产环境对接阿里云短信（sms.provider=aliyun）。</p>
 * <p>阿里云短信为"签名 + 模板CODE + 模板参数"模式：签名与模板文案在阿里云
 * 平台侧报备，服务端不拼裸短信内容。泛化模板签名供未来通知类短信
 * （notify_template 已预留 SMS 渠道）复用同一通道。</p>
 */
public interface SmsSender {

    /**
     * 发送模板短信
     *
     * @param phone        手机号
     * @param templateCode 短信模板 CODE（如阿里云 SMS_XXXXXX）
     * @param params       模板参数（如 {"code":"123456"}），key 与平台侧报备的模板变量一致
     * @return true 发送成功，false 发送失败
     */
    boolean send(String phone, String templateCode, Map<String, String> params);
}
