package com.atmoto.recruit.biz.portal.dto;

import lombok.Data;

/**
 * 刷新 Token 请求 DTO
 */
@Data
public class RefreshTokenRequest {

    /** 刷新令牌 */
    private String refreshToken;
}
