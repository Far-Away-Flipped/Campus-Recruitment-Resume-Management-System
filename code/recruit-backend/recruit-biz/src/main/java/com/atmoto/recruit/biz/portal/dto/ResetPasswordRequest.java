package com.atmoto.recruit.biz.portal.dto;

import lombok.Data;

/**
 * 重置密码请求 DTO
 */
@Data
public class ResetPasswordRequest {

    /** 手机号 */
    private String phone;

    /** 短信验证码 */
    private String smsCode;

    /** 新密码 */
    private String newPassword;
}
