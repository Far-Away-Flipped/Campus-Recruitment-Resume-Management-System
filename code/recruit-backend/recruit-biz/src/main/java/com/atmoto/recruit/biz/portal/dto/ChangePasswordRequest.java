package com.atmoto.recruit.biz.portal.dto;

import lombok.Data;

/**
 * 修改密码请求 DTO
 */
@Data
public class ChangePasswordRequest {

    /** 原密码 */
    private String oldPassword;

    /** 新密码 */
    private String newPassword;
}
