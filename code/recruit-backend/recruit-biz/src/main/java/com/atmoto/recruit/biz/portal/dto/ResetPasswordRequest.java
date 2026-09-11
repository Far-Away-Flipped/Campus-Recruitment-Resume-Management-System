package com.atmoto.recruit.biz.portal.dto;

import lombok.Data;

/**
 * 重置密码请求 DTO
 */
@Data
public class ResetPasswordRequest {

    /** 手机号（登录账号） */
    private String phone;

    /** 绑定邮箱（验证码接收地址，须与账号注册邮箱一致） */
    private String email;

    /** 邮箱验证码 */
    private String smsCode;

    /** 新密码 */
    private String newPassword;
}
