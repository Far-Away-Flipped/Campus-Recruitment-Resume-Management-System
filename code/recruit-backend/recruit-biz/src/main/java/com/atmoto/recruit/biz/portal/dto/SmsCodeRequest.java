package com.atmoto.recruit.biz.portal.dto;

import lombok.Data;

/**
 * 发送短信验证码请求 DTO
 */
@Data
public class SmsCodeRequest {

    /** 手机号 */
    private String phone;

    /** 图形验证码key */
    private String captchaKey;

    /** 图形验证码 */
    private String captchaCode;
}
