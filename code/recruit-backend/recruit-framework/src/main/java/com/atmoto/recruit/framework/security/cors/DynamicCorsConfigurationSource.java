package com.atmoto.recruit.framework.security.cors;

import com.atmoto.recruit.system.domain.CorsWhitelistRule;
import com.atmoto.recruit.system.service.ICorsWhitelistService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 动态 CORS 匹配源 —— 每次请求实时查询有效白名单规则并构造 CorsConfiguration
 * <p>
 * 替代 {@code UrlBasedCorsConfigurationSource} 的"注册一次，全程复用"静态模型：
 * 该类本身非线程安全，不适合运行时反复刷新白名单，因此自定义 CorsConfigurationSource
 * 在每次请求时实时构造。详见《HR 后台「网络管理」模块设计方案 V1.0》第 3.4/3.5 节。
 * </p>
 * <p>
 * 匹配算法（方案 3.5 节【发现B】显式定义，不可自行改写）：
 * EXACT 类型做大小写不敏感的完整 Origin 精确匹配；CIDR 类型仅对"字面 IPv4 地址"的
 * Origin host 做网段包含判断，域名类 Origin（如 campus.atmoto.cn）绝不触发 CIDR/网络
 * 地址解析逻辑，只走精确匹配——避免对域名做 DNS 解析后判断引入延迟与潜在 SSRF 风险面，
 * 或把域名字符串直接传入 CIDR 判断库导致异常/误判。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicCorsConfigurationSource implements CorsConfigurationSource {

    /** IPv4 字面量正则：仅用于判断 Origin host 是否为"字面IP地址"，不做合法性以外的语义校验 */
    private static final Pattern IPV4_LITERAL =
            Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");

    private final ICorsWhitelistService corsWhitelistService;

    @Override
    public CorsConfiguration getCorsConfiguration(@NonNull HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            // 无 Origin 头（非跨域请求，如同源请求/curl不带Origin）—— 不构造CORS配置
            return null;
        }

        List<CorsWhitelistRule> rules = corsWhitelistService.getEffectiveRules();
        boolean matched = false;
        for (CorsWhitelistRule rule : rules) {
            if (matchOrigin(rule, origin)) {
                matched = true;
                break;
            }
        }
        if (!matched) {
            return null;
        }

        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin(origin);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        return config;
    }

    /**
     * Origin 匹配算法（方案 3.5 节原文，逐字实现，不可改写）
     */
    private boolean matchOrigin(CorsWhitelistRule rule, String origin) {
        if ("EXACT".equals(rule.getRuleType())) {
            return rule.getRuleValue().equalsIgnoreCase(origin);
        }
        // CIDR类型：仅对"字面IP地址"的Origin host做网段包含判断
        // 域名类Origin（如campus.atmoto.cn）只走精确匹配，绝不触发CIDR/网络地址解析逻辑
        try {
            String host = URI.create(origin).getHost();
            if (host == null || !IPV4_LITERAL.matcher(host).matches()) {
                return false;
            }
            return new IpAddressMatcher(rule.getRuleValue()).matches(host);
        } catch (Exception e) {
            // Origin格式异常 → 拒绝而非抛异常中断请求链
            log.debug("CORS Origin格式解析异常，拒绝匹配：origin={}, rule={}", origin, rule.getRuleValue(), e);
            return false;
        }
    }
}
