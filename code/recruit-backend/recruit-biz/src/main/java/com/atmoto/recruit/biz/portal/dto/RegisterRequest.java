package com.atmoto.recruit.biz.portal.dto;

import lombok.Data;

/**
 * 注册请求 DTO
 */
@Data
public class RegisterRequest {

    /** 手机号 */
    private String phone;

    /** 短信验证码 */
    private String smsCode;

    /** 密码 */
    private String password;

    /** 隐私协议同意 */
    private Boolean privacyAgreed;
}
