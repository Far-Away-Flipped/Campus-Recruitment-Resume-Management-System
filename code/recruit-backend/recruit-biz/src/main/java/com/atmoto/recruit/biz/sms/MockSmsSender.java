package com.atmoto.recruit.biz.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 模拟短信发送器（开发环境，sms.provider=mock 或未配置时生效）
 * <p>通过日志打印模板参数，不实际调用短信网关。开发调试时从后端日志
 * 读取验证码，接口不向前端回传。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "sms", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsSender implements SmsSender {

    @Override
    public boolean send(String phone, String templateCode, Map<String, String> params) {
        log.info("【模拟短信】发送至 {}（模板 {}）：{}", phone, templateCode, params);
        return true;
    }
}
