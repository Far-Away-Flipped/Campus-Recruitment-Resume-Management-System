package com.atmoto.recruit.biz.sms.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 短信配置注册
 * <p>项目未启用 @ConfigurationPropertiesScan，需在此显式注册 SmsProperties</p>
 */
@Configuration
@EnableConfigurationProperties(SmsProperties.class)
public class SmsConfig {
}
