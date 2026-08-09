package com.atmoto.recruit.framework.web.service;

import com.github.benmanes.caffeine.cache.Cache;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * HR 后台 Token 签发与校验服务（Caffeine 实现，替代 Redis）
 */
@Slf4j
@Service
public class AdminTokenService {

    @Value("${jwt.admin.secret}")
    private String adminSecret;

    @Value("${jwt.admin.expiration:30}")
    private int expirationMinutes;

    private final Cache<String, Object> tokenCache;

    public AdminTokenService(@Qualifier("tokenCache") Cache<String, Object> tokenCache) {
        this.tokenCache = tokenCache;
    }

    /** 签发 Token */
    public String createToken(Long userId, String username) {
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiration = new Date(now + expirationMinutes * 60 * 1000L);

        String token = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim("userId", userId)
                .claim("aud", "admin")
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(getKey())
                .compact();

        // 存入 Caffeine，实现主动吊销能力
        tokenCache.put("login_tokens:" + token, userId);
        return token;
    }

    /** 解析 Token */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Token 是否仍然有效（未被吊销） */
    public boolean isTokenValid(String token) {
        return tokenCache.getIfPresent("login_tokens:" + token) != null;
    }

    /** 吊销 Token */
    public void revokeToken(String token) {
        tokenCache.invalidate("login_tokens:" + token);
    }

    private SecretKey getKey() {
        return new SecretKeySpec(
                Base64.getDecoder().decode(adminSecret.getBytes(StandardCharsets.UTF_8)),
                "HmacSHA256");
    }
}
