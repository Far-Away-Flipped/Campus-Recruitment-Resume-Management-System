package com.atmoto.recruit.biz.sms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 短信服务配置
 * <p>provider 切换发送实现：mock（日志打印，开发环境默认）| aliyun（真实短信）。
 * aliyun 的 AK/签名/模板码由甲方在阿里云控制台完成实名认证与报备后，
 * 经环境变量注入（见 application-prod.yml 与 .env.example）。</p>
 */
@Data
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    /** 发送实现：mock | aliyun */
    private String provider = "mock";

    private Aliyun aliyun = new Aliyun();

    @Data
    public static class Aliyun {
        /** 阿里云 AccessKey ID（环境变量 SMS_ACCESS_KEY_ID 注入） */
        private String accessKeyId;
        /** 阿里云 AccessKey Secret（环境变量 SMS_ACCESS_KEY_SECRET 注入） */
        private String accessKeySecret;
        /** 短信签名（阿里云平台侧报备，如"遨天科技"） */
        private String signName = "遨天科技";
        /** 验证码模板 CODE（阿里云平台侧报备，如 SMS_000000） */
        private String templateCode;
        /** API 端点 */
        private String endpoint = "dysmsapi.aliyuncs.com";
    }
}
