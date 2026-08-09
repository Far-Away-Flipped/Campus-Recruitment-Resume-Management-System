package com.atmoto.recruit.biz.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 模拟短信发送器（开发环境使用）
 * <p>通过日志打印验证码，不实际调用短信网关</p>
 */
@Slf4j
@Component
public class MockSmsSender implements SmsSender {

    @Override
    public boolean send(String phone, String content) {
        log.info("【模拟短信】发送至 {}：{}", phone, content);
        return true;
    }
}
