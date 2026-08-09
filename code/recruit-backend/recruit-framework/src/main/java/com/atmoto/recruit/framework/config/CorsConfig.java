package com.atmoto.recruit.framework.config;

import com.atmoto.recruit.framework.security.cors.DynamicCorsConfigurationSource;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * CORS 跨域配置（前后端分离必需）
 * <p>
 * 安全策略：CorsFilter 的匹配源改为 {@link DynamicCorsConfigurationSource}，
 * 白名单规则从 sys_cors_origin 表动态读取（Caffeine 缓存 + 数据库双失效降级），
 * HR 后台修改后无需重启即可生效。详见《HR 后台「网络管理」模块设计方案 V1.0》第 3.4 节。
 * </p>
 * <p>
 * DEFAULT_ORIGINS 常量角色从"唯一白名单来源"降格为"数据库/缓存双失效时的最后防线"，
 * 由 {@code CorsWhitelistServiceImpl} 在降级路径中使用（该常量的内容副本，详见其类注释）。
 * 同时作为 init-data.sql 的种子数据插入 sys_cors_origin（is_builtin=1）。
 * </p>
 * <p>
 * 【测试发现的阻塞级问题，本次一并修复】普通 {@code @Bean CorsFilter} 默认以
 * {@code Ordered.LOWEST_PRECEDENCE} 注册，晚于 Spring Security 的 {@code FilterChainProxy}
 * （默认注册在靠前的 {@code -100}）。浏览器跨域请求触发的 OPTIONS 预检本身不携带
 * Authorization 头，会先被 {@code adminFilterChain}/{@code portalFilterChain} 内的
 * {@code AdminTokenFilter}/{@code PortalTokenFilter} 拦截判定为未登录并直接返回
 * {@code 401/20001} 响应——CorsFilter 根本没有机会先设置 CORS 响应头。浏览器收到不含
 * {@code Access-Control-*} 头的预检响应会判定为跨域失败，真实请求根本不会发出，
 * 在受保护端点（{@code adminFilterChain} 下 anyRequest().authenticated() 覆盖的路径，
 * 例如本模块 /api/system/network/**）上复现为"即使Origin已在白名单、真实浏览器仍连不上"。
 * 修复：用 {@link FilterRegistrationBean} 包装并显式设为 {@link Ordered#HIGHEST_PRECEDENCE}，
 * 确保 CorsFilter 在 Spring Security 过滤器链之前执行，OPTIONS 预检在到达鉴权层前
 * 就已经被正确处理并附带 CORS 响应头。
 * </p>
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /** 默认安全白名单（数据库/缓存双失效时的降级兜底，见类注释） */
    public static final List<String> DEFAULT_ORIGINS = List.of(
        "http://localhost:5173",
        "http://localhost:5174",
        "http://127.0.0.1:5173",
        "http://127.0.0.1:5174",
        "https://campus.atmoto.cn"
    );

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter(DynamicCorsConfigurationSource dynamicCorsConfigurationSource) {
        FilterRegistrationBean<CorsFilter> registration =
                new FilterRegistrationBean<>(new CorsFilter(dynamicCorsConfigurationSource));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}

