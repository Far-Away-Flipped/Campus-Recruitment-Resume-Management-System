package com.atmoto.recruit.framework.web.service;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * 学生端 Portal Token 签发与校验服务
 */
@Slf4j
@Service
public class PortalTokenService {

    @Value("${jwt.portal.secret}")
    private String portalSecret;

    @Value("${jwt.portal.access-expiration:120}")
    private int accessExpirationMinutes;

    @Value("${jwt.portal.refresh-expiration:10080}")
    private int refreshExpirationMinutes; // 7天 = 10080分钟

    /** 签发 Access Token（2小时） */
    public String createAccessToken(Long studentId, String phone) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(studentId.toString())      // sub=studentId（无 PII 泄露）
                .claim("studentId", studentId)
                .claim("aud", "portal")
                .claim("type", "access")
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessExpirationMinutes * 60 * 1000L))
                .signWith(getKey())
                .compact();
    }

    /** 签发 Refresh Token（7天）- 入库，可吊销 */
    public String createRefreshToken(Long studentId, String phone) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(studentId.toString())      // sub=studentId（无 PII 泄露）
                .claim("studentId", studentId)
                .claim("aud", "portal")
                .claim("type", "refresh")
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshExpirationMinutes * 60 * 1000L))
                .signWith(getKey())
                .compact();
    }

    private SecretKey getKey() {
        return new SecretKeySpec(
                Base64.getDecoder().decode(portalSecret.getBytes(StandardCharsets.UTF_8)),
                "HmacSHA256");
    }
}
