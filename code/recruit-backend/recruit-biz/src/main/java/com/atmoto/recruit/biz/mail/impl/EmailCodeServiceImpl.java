package com.atmoto.recruit.biz.mail.impl;

import com.atmoto.recruit.biz.mail.EmailCodeService;
import com.atmoto.recruit.biz.mail.EmailSender;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.SecureRandom;
import java.time.LocalDate;

/**
 * 邮箱验证码服务实现
 * <p>与 SmsCodeServiceImpl 同款防刷策略（key 换为邮箱）：</p>
 * <p>
 * 1. 同邮箱60秒内只能发1次<br>
 * 2. 同IP 60秒内最多3次<br>
 * 3. 同邮箱日上限10条<br>
 * 4. 图形验证码前置（由 PortalAuthServiceImpl 校验）<br>
 * 5. 发送成功后才写防刷计数（失败不占额度，可立即重试）<br>
 * 6. 验证码一次性消费
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCodeServiceImpl implements EmailCodeService {

    private final EmailSender emailSender;

    @Qualifier("emailCodeCache")
    private final Cache<String, String> emailCodeCache;

    @Qualifier("emailRateCache")
    private final Cache<String, int[]> emailRateCache;

    @Qualifier("emailDailyCache")
    private final Cache<String, int[]> emailDailyCache;

    /** 每日发送上限 */
    private static final int DAILY_LIMIT = 10;

    /** 同IP 60秒内最多发送次数 */
    private static final int IP_RATE_LIMIT = 3;

    /** 验证码有效期（分钟） */
    private static final int CODE_EXPIRE_MINUTES = 5;

    /** 安全随机数生成器 */
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void sendCode(String email) {
        // ── 防刷1：同邮箱60秒内只能发1次（只读检查，计数在发送成功后才写入） ──
        String emailRateKey = "email_rate:" + email;
        if (emailRateCache.getIfPresent(emailRateKey) != null) {
            throw new BizException(ErrorCode.RATE_LIMITED, "60秒内仅可发送1次验证码");
        }

        // ── 防刷2：同IP 60秒内最多3次（只读检查，与短信共用 IP 维度限流键空间但计数独立） ──
        String clientIp = getClientIp();
        String ipRateKey = "ip_rate:" + clientIp;
        int[] ipRate = emailRateCache.getIfPresent(ipRateKey);
        if (ipRate != null && ipRate[0] >= IP_RATE_LIMIT) {
            throw new BizException(ErrorCode.RATE_LIMITED, "当前IP发送过于频繁，请稍后再试");
        }

        // ── 防刷3：同邮箱日上限10条（只读检查） ──
        String dailyKey = "email_daily:" + email + ":" + LocalDate.now();
        int[] dailyCount = emailDailyCache.getIfPresent(dailyKey);
        if (dailyCount != null && dailyCount[0] >= DAILY_LIMIT) {
            throw new BizException(ErrorCode.RATE_LIMITED, "今日验证码发送已达上限（" + DAILY_LIMIT + "条）");
        }

        // ── 生成6位随机验证码，存入Caffeine缓存（5分钟过期） ──
        String code = String.format("%06d", RANDOM.nextInt(1000000));
        emailCodeCache.put(email, code);

        // ── 发送邮件（发送在一切防刷计数写入之前：失败不占额度、不上60s锁） ──
        boolean sent = emailSender.sendCodeEmail(email, code);
        if (!sent) {
            log.error("验证码邮件发送失败：email={}", email);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "验证码邮件发送失败，请稍后再试");
        }

        // ── 发送成功，写入防刷计数 ──
        emailRateCache.put(emailRateKey, new int[]{1});
        if (ipRate == null) {
            emailRateCache.put(ipRateKey, new int[]{1});
        } else {
            ipRate[0]++;
        }
        if (dailyCount == null) {
            emailDailyCache.put(dailyKey, new int[]{1});
        } else {
            dailyCount[0]++;
        }

        log.info("邮箱验证码已发送：email={}", email);
    }

    @Override
    public boolean verifyCode(String email, String code) {
        String cachedCode = emailCodeCache.getIfPresent(email);
        if (cachedCode == null) {
            return false;
        }
        if (cachedCode.equals(code)) {
            // 验证成功 → 消费验证码（一次性）
            emailCodeCache.invalidate(email);
            return true;
        }
        return false;
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "unknown";
            }
            var request = attrs.getRequest();
            // 尝试从代理头获取真实IP
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
