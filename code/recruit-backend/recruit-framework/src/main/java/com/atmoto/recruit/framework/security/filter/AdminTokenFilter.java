package com.atmoto.recruit.framework.security.filter;

import com.atmoto.recruit.framework.security.context.AdminUserHolder;
import com.atmoto.recruit.framework.web.service.AdminTokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * HR后台 JWT Token 过滤器 —— adminFilterChain
 * <p>
 * 使用后台独立签名密钥（jwt.admin.secret），与学生端密钥不同。
 * 学生token在此链上验签即失败，从结构上阻断垂直越权。
 * </p>
 */
@Slf4j
@Component
public class AdminTokenFilter extends OncePerRequestFilter {

    @Autowired
    private AdminTokenService adminTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain)
            throws ServletException, IOException {

        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = adminTokenService.parseToken(token);

            // 验证 aud 必须是 admin（注意：JJWT 会将单值 aud 转为 Set，不能用 String.class 获取）
            java.util.Set<String> audience = claims.getAudience();
            if (audience == null || !audience.contains("admin")) {
                chain.doFilter(request, response);
                return;
            }

            // 存入 Caffeine 缓存验证是否仍有效（token可被主动吊销）
            if (!adminTokenService.isTokenValid(token)) {
                chain.doFilter(request, response);
                return;
            }

            // 从Token提取当前HR用户信息，存入ThreadLocal供业务层使用
            Long userId = claims.get("userId", Long.class);
            String username = claims.getSubject();
            AdminUserHolder.set(userId, username);

            // 创建 Spring Security 认证对象，设置到 SecurityContext
            // 使 SecurityConfig 的 .anyRequest().authenticated() 能够通过
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (Exception e) {
            log.debug("Admin token validation failed: {}", e.getMessage());
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // 清理 ThreadLocal，防止内存泄漏
            AdminUserHolder.clear();
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
