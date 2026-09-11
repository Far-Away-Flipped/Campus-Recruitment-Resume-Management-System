package com.atmoto.recruit.biz.portal.dto;

import lombok.Data;

/**
 * 发送邮箱验证码请求 DTO
 * <p>scene=register：新手机号注册（手机号可为未注册，直接向邮箱发码）；
 * scene=reset：重置密码（按手机号定位账号并校验邮箱匹配，防枚举：
 * 账号不存在/邮箱不匹配均静默返回已发送）。</p>
 */
@Data
public class EmailCodeRequest {

    /** 场景：register（注册，默认）| reset（重置密码） */
    private String scene;

    /** 手机号（登录账号，定位用） */
    private String phone;

    /** 邮箱（验证码接收地址） */
    private String email;

    /** 图形验证码缓存key */
    private String captchaKey;

    /** 图形验证码 */
    private String captchaCode;
}
