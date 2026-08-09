package com.atmoto.recruit.framework.config;

import com.atmoto.recruit.framework.security.cors.DynamicCorsConfigurationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public CorsFilter corsFilter(DynamicCorsConfigurationSource dynamicCorsConfigurationSource) {
        return new CorsFilter(dynamicCorsConfigurationSource);
    }
}

