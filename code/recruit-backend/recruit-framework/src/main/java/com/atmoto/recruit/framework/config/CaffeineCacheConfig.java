package com.atmoto.recruit.framework.config;

import com.atmoto.recruit.system.domain.CorsWhitelistRule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置 —— 替代 RuoYi 原 Redis 缓存
 * <p>
 * 本类替代 RedisConfig + RedisCache，单机 Caffeine 实现，不可用于多实例部署。
 * 缓存实例：
 * - tokenCache：HR登录Token存储（30min滑动过期，最多500条）
 * - captchaCache：图形验证码（2min写入后过期）
 * - configCache：系统参数配置（无过期，主动evict）
 * - dictCache：字典数据（无过期，主动evict）
 * - repeatSubmitCache：防重复提交（10s写入后过期）
 * - rateLimitCache：接口限流计数（60s写入后过期）
 * - loginFailCache：登录失败计数（15min写入后过期）
 * - smsRateCache：短信发送频率（60s写入后过期）
 * - corsWhitelistCache：CORS动态白名单规则整表缓存（无过期，写操作后主动invalidate）
 * </p>
 */
@Configuration
@EnableCaching
public class CaffeineCacheConfig {

    // ────────── Token ──────────

    @Bean("tokenCache")
    public Cache<String, Object> tokenCache() {
        return Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();
    }

    // ────────── 验证码 ──────────

    @Bean("captchaCache")
    public Cache<String, Object> captchaCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    // ────────── 系统配置 ──────────

    @Bean("configCache")
    public Cache<String, Object> configCache() {
        return Caffeine.newBuilder()
                .maximumSize(200)
                .build();
    }

    // ────────── 字典 ──────────

    @Bean("dictCache")
    public Cache<String, Object> dictCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .build();
    }

    // ────────── 防重复提交 ──────────

    @Bean("repeatSubmitCache")
    public Cache<String, Boolean> repeatSubmitCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .maximumSize(2000)
                .build();
    }

    // ────────── 限流 ──────────

    @Bean("rateLimitCache")
    public Cache<String, java.util.concurrent.atomic.AtomicInteger> rateLimitCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(5000)
                .build();
    }

    // ────────── 登录失败计数 ──────────

    @Bean("loginFailCache")
    public Cache<String, Integer> loginFailCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    // ────────── 短信频率 ──────────

    @Bean("smsRateCache")
    public Cache<String, int[]> smsRateCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(2000)
                .build();
    }

    // ────────── 短信验证码存储 ──────────

    /**
     * 短信验证码缓存 —— 写入后5分钟过期
     * <p>Key: 手机号，Value: 6位验证码</p>
     */
    @Bean("smsCodeCache")
    public Cache<String, String> smsCodeCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(5000)
                .build();
    }

    // ────────── 短信日发送计数 ──────────

    /**
     * 短信日发送次数缓存 —— 写入后24小时过期
     * <p>Key: sms_daily:手机号:日期，Value: int[1]计数</p>
     */
    @Bean("smsDailyCache")
    public Cache<String, int[]> smsDailyCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .maximumSize(5000)
                .build();
    }

    // ────────── 导出日累计计数 ──────────

    /**
     * 导出日累计计数缓存 —— 写入后24小时过期
     * <p>Key: export:daily:userId:date，Value: int[1]计数</p>
     */
    @Bean("exportCountCache")
    public Cache<String, int[]> exportCountCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .maximumSize(2000)
                .build();
    }

    // ────────── 文件预览一次性Ticket ──────────

    /**
     * 附件预览一次性Ticket缓存 —— 写入后60秒过期
     * <p>Key: ticket UUID，Value: 文件ID（Long）</p>
     */
    @Bean("previewTicketCache")
    public Cache<String, Long> previewTicketCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(5000)
                .build();
    }

    // ────────── CORS 动态白名单 ──────────

    /**
     * CORS 白名单规则整表缓存 —— 无TTL，maximumSize=1（整表作为单一条目缓存，key固定）
     * <p>读路径：getEffectiveRules() 优先查缓存，miss 则查 DB 并回填；
     * 写路径：先写DB成功 → 再 invalidate 缓存，不做增量更新，下次读取触发整表重载。
     * 详见《HR 后台「网络管理」模块设计方案 V1.0》第 3.6 节。</p>
     */
    @Bean("corsWhitelistCache")
    public Cache<String, List<CorsWhitelistRule>> corsWhitelistCache() {
        return Caffeine.newBuilder()
                .maximumSize(1)
                .build();
    }
}
