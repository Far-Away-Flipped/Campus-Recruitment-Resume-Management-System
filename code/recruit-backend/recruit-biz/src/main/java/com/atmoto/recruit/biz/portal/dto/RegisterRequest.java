package com.atmoto.recruit.biz.portal.dto;

import lombok.Data;

/**
 * 注册请求 DTO
 */
@Data
public class RegisterRequest {

    /** 手机号（登录账号） */
    private String phone;

    /** 邮箱（验证码接收地址，注册时绑定） */
    private String email;

    /** 邮箱验证码 */
    private String smsCode;

    /** 密码 */
    private String password;

    /** 隐私协议同意 */
    private Boolean privacyAgreed;
}
