package com.atmoto.recruit.biz.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token 响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    /** 访问令牌（2小时有效） */
    private String accessToken;

    /** 刷新令牌（7天有效） */
    private String refreshToken;

    public static TokenResponse of(String accessToken, String refreshToken) {
        return new TokenResponse(accessToken, refreshToken);
    }
}
