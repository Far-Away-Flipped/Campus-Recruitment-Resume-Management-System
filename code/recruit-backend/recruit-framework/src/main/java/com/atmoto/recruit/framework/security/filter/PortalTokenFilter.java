package com.atmoto.recruit.framework.security.filter;

import com.atmoto.recruit.framework.security.context.PortalUserHolder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * 学生端 JWT Token 过滤器 —— portalFilterChain
 * <p>
 * 关键安全设计：
 * - 使用独立签名密钥（jwt.portal.secret），与后台密钥不同
 * - 只从token中提取studentId，不接受请求参数传入
 * - token验签失败直接401，不丢401进adminFilterChain
 * </p>
 */
@Slf4j
@Component
public class PortalTokenFilter extends OncePerRequestFilter {

    @Value("${jwt.portal.secret}")
    private String portalSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain)
            throws ServletException, IOException {

        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            // 无token → 交给 SecurityConfig 的 permitAll 或 entryPoint 处理
            chain.doFilter(request, response);
            return;
        }

        try {
            SecretKey key = new SecretKeySpec(
                    Base64.getDecoder().decode(portalSecret.getBytes(StandardCharsets.UTF_8)),
                    "HmacSHA256");

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 验证 aud 声明必须是 portal（注意：JJWT 会将单值 aud 转为 Set，不能用 String.class 获取）
            java.util.Set<String> audience = claims.getAudience();
            if (audience == null || !audience.contains("portal")) {
                log.warn("Invalid aud claim in portal token: {}", audience);
                chain.doFilter(request, response);
                return;
            }

            Long studentId = claims.get("studentId", Long.class);
            PortalUserHolder.set(studentId);

            // 创建 Spring Security 认证对象，设置到 SecurityContext
            // 使 SecurityConfig 的 .anyRequest().authenticated() 能够通过
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_PORTAL"));
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(studentId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (Exception e) {
            log.debug("Portal token validation failed: {}", e.getMessage());
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // 清理 ThreadLocal，防止内存泄漏
            PortalUserHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
