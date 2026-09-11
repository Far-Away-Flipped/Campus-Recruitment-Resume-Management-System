package com.atmoto.recruit.biz.sms.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.atmoto.recruit.biz.sms.SmsSender;
import com.atmoto.recruit.biz.sms.config.SmsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 阿里云短信发送器（生产环境）
 * <p>由 sms.provider=aliyun 激活。签名与模板在阿里云平台侧报备，
 * 此处仅传模板 CODE + 模板参数，不拼裸短信内容。</p>
 * <p>fail-closed：AK/签名/模板码任一缺失时拒绝启动，
 * 绝不带空凭证运行到发送时才失败。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sms", name = "provider", havingValue = "aliyun")
public class AliyunSmsSender implements SmsSender {

    private final SmsProperties smsProperties;
    private final ObjectMapper objectMapper;

    private Client client;

    @PostConstruct
    void init() throws Exception {
        SmsProperties.Aliyun aliyun = smsProperties.getAliyun();
        // fail-closed：provider=aliyun 但凭证不全 → 启动即失败
        if (isBlank(aliyun.getAccessKeyId()) || isBlank(aliyun.getAccessKeySecret())
                || isBlank(aliyun.getSignName()) || isBlank(aliyun.getTemplateCode())) {
            throw new IllegalStateException(
                    "sms.provider=aliyun 但 access-key/sign-name/template-code 未配置，拒绝启动");
        }
        Config config = new Config()
                .setAccessKeyId(aliyun.getAccessKeyId())
                .setAccessKeySecret(aliyun.getAccessKeySecret())
                .setEndpoint(aliyun.getEndpoint());
        this.client = new Client(config);
        log.info("阿里云短信发送器已初始化：signName={}, templateCode={}",
                aliyun.getSignName(), aliyun.getTemplateCode());
    }

    @Override
    public boolean send(String phone, String templateCode, Map<String, String> params) {
        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(smsProperties.getAliyun().getSignName())
                    .setTemplateCode(templateCode)
                    .setTemplateParam(objectMapper.writeValueAsString(params));
            SendSmsResponse response = client.sendSms(request);
            if (response.getBody() != null && "OK".equals(response.getBody().getCode())) {
                log.info("短信发送成功：phone={}, templateCode={}", phone, templateCode);
                return true;
            }
            // 业务失败：限流/欠费/签名模板无效等，打码供运维排查，不外抛细节
            log.error("短信发送失败：phone={}, Code={}, Message={}",
                    phone,
                    response.getBody() != null ? response.getBody().getCode() : "null",
                    response.getBody() != null ? response.getBody().getMessage() : "null");
            return false;
        } catch (Exception e) {
            log.error("短信发送异常：phone={}, templateCode={}, error={}", phone, templateCode, e.getMessage());
            return false;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
