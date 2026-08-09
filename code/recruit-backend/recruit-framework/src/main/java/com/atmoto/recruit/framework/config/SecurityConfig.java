package com.atmoto.recruit.framework.config;

import com.atmoto.recruit.framework.security.filter.AdminTokenFilter;
import com.atmoto.recruit.framework.security.filter.PortalTokenFilter;
import com.atmoto.recruit.framework.security.handle.AdminAuthEntryPoint;
import com.atmoto.recruit.framework.security.handle.PortalAuthEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 双 SecurityFilterChain 配置
 * <p>
 * 关键设计：
 * - portalFilterChain（Order 1）：学生端 /api/portal/**，使用双密钥JWT，公网可访问
 * - adminFilterChain（Order 2）：HR后台 /api/admin/** + 系统管理 /api/system/**
 * <p>
 * 为什么分离认证体系：学生Token在admin链上验签即失败——
 * 防越权从"依赖注解写对"变为"结构上不可能"。
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private PortalTokenFilter portalTokenFilter;

    @Autowired
    private AdminTokenFilter adminTokenFilter;

    @Autowired
    private PortalAuthEntryPoint portalAuthEntryPoint;

    @Autowired
    private AdminAuthEntryPoint adminAuthEntryPoint;

    /** 学生端过滤链 —— 先匹配（Order 1） */
    @Bean
    @Order(1)
    public SecurityFilterChain portalFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/portal/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 学生端匿名可访问：注册、登录、Token刷新、验证码、短信码、重置密码、岗位浏览、品牌配置
                .requestMatchers(
                    "/api/portal/auth/login",
                    "/api/portal/auth/register",
                    "/api/portal/auth/refresh",
                    "/api/portal/auth/captcha",
                    "/api/portal/auth/sms-code",
                    "/api/portal/auth/reset-password",
                    "/api/portal/jobs/**",
                    "/api/portal/brand/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(portalAuthEntryPoint)
            )
            .addFilterBefore(portalTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** HR端 + 系统管理端过滤链 —— 兜底匹配（Order 2） */
    @Bean
    @Order(2)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 后台登录与验证码匿名可访问
                .requestMatchers(
                    "/api/admin/auth/login",
                    "/api/admin/auth/captcha"
                ).permitAll()
                // 健康检查
                .requestMatchers("/api/health").permitAll()
                // 系统管理对内的接口
                .requestMatchers("/api/system/auth/login").permitAll()
                // Actuator 需要认证（防御纵深：即使 server.address=127.0.0.1 也不依赖网络层隔离）
                .requestMatchers("/actuator/**").authenticated()
                // 公共文件预览 —— 通过一次性ticket鉴权，无需登录Token
                .requestMatchers("/api/common/file/preview").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(adminAuthEntryPoint)
            )
            .addFilterBefore(adminTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
