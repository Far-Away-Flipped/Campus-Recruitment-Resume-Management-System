package com.atmoto.recruit.biz.portal.dto;

import lombok.Data;

/**
 * 登录请求 DTO
 */
@Data
public class LoginRequest {

    /** 手机号 */
    private String phone;

    /** 密码 */
    private String password;
}
