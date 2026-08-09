package com.atmoto.recruit.framework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源配置 —— 与 CORS 配置互补
 * <p>CORS 由 CorsConfig 中的 CorsFilter Bean（DynamicCorsConfigurationSource）统一处理，
 * 本类不得再注册任何 CORS 相关配置。</p>
 * <p>
 * 【阻塞级清理】原 addCorsMappings("/**") 覆写（allowedOriginPatterns("*") + allowCredentials(true)）
 * 已删除——这段 RuoYi 脚手架遗留代码与 CorsConfig 的白名单 CorsFilter Bean 同时存在于同一容器，
 * 生效顺序不保证一致，一旦生效会使动态白名单机制整体被架空，退化为"任意 Origin + 允许携带凭证"。
 * 详见《HR 后台「网络管理」模块设计方案 V1.0》第 6.1 节【发现A・阻塞级】。
 * </p>
 */
@Configuration
public class ResourcesConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态资源映射（生产环境由 Nginx 处理，开发环境由 Spring Boot 兜底）
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}

