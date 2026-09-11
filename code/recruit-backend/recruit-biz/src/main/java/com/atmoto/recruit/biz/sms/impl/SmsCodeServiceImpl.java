package com.atmoto.recruit.biz.sms.impl;

import com.atmoto.recruit.biz.sms.SmsCodeService;
import com.atmoto.recruit.biz.sms.SmsSender;
import com.atmoto.recruit.biz.sms.config.SmsProperties;
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
import java.util.Map;

/**
 * 短信验证码服务实现
 * <p>
 * 六层防刷策略：
 * 1. 同手机号60秒内只能发1次
 * 2. 同IP 60秒内最多3次
 * 3. 同手机号日上限10条
 * 4. 必须先过图形验证码（由Controller层保证）
 * 5. 短信频率缓存记录发送次数
 * 6. 验证码一次性消费
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeServiceImpl implements SmsCodeService {

    private final SmsSender smsSender;

    private final SmsProperties smsProperties;

    @Qualifier("smsCodeCache")
    private final Cache<String, String> smsCodeCache;

    @Qualifier("smsRateCache")
    private final Cache<String, int[]> smsRateCache;

    @Qualifier("captchaCache")
    private final Cache<String, Object> captchaCache;

    @Qualifier("smsDailyCache")
    private final Cache<String, int[]> smsDailyCache;

    /** 每日发送上限 */
    private static final int DAILY_LIMIT = 10;

    /** 同IP 60秒内最多发送次数 */
    private static final int IP_RATE_LIMIT = 3;

    /** 同手机号发送间隔（秒） */
    private static final int PHONE_INTERVAL_SECONDS = 60;

    /** 验证码有效期（分钟） */
    private static final int CODE_EXPIRE_MINUTES = 5;

    /** 安全随机数生成器 */
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void sendCode(String phone) {
        // ── 防刷1：同手机号60秒内只能发1次（只读检查，计数在发送成功后才写入） ──
        String phoneRateKey = "phone_rate:" + phone;
        if (smsRateCache.getIfPresent(phoneRateKey) != null) {
            throw new BizException(ErrorCode.RATE_LIMITED, "60秒内仅可发送1次验证码");
        }

        // ── 防刷2：同IP 60秒内最多3次（只读检查） ──
        String clientIp = getClientIp();
        String ipRateKey = "ip_rate:" + clientIp;
        int[] ipRate = smsRateCache.getIfPresent(ipRateKey);
        if (ipRate != null && ipRate[0] >= IP_RATE_LIMIT) {
            throw new BizException(ErrorCode.RATE_LIMITED, "当前IP发送过于频繁，请稍后再试");
        }

        // ── 防刷3：同手机号日上限10条（只读检查） ──
        String dailyKey = "sms_daily:" + phone + ":" + LocalDate.now();
        int[] dailyCount = smsDailyCache.getIfPresent(dailyKey);
        if (dailyCount != null && dailyCount[0] >= DAILY_LIMIT) {
            throw new BizException(ErrorCode.RATE_LIMITED, "今日验证码发送已达上限（" + DAILY_LIMIT + "条）");
        }

        // ── 生成6位随机验证码，存入Caffeine缓存（5分钟过期） ──
        String code = String.format("%06d", RANDOM.nextInt(1000000));
        smsCodeCache.put(phone, code);

        // ── 发送短信（模板码+参数，正文由平台侧模板渲染；mock 走日志） ──
        // 发送在一切防刷计数写入之前执行：发送失败不占防刷额度、不上60s锁，用户可立即重试
        boolean sent = smsSender.send(phone, smsProperties.getAliyun().getTemplateCode(),
                Map.of("code", code));
        if (!sent) {
            log.error("短信发送失败：phone={}", phone);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "短信发送失败，请稍后再试");
        }

        // ── 发送成功，写入防刷计数 ──
        // 手机号频率标记，60秒后过期（由 smsRateCache 的 expireAfterWrite 控制）
        smsRateCache.put(phoneRateKey, new int[]{1});
        if (ipRate == null) {
            smsRateCache.put(ipRateKey, new int[]{1});
        } else {
            ipRate[0]++;
        }
        if (dailyCount == null) {
            smsDailyCache.put(dailyKey, new int[]{1});
        } else {
            dailyCount[0]++;
        }

        log.info("验证码已发送：phone={}", phone);
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        String cachedCode = smsCodeCache.getIfPresent(phone);
        if (cachedCode == null) {
            return false;
        }
        if (cachedCode.equals(code)) {
            // 验证成功 → 消费验证码（一次性）
            smsCodeCache.invalidate(phone);
            return true;
        }
        return false;
    }

    @Override
    public void verifyCaptcha(String captchaKey, String captchaCode) {
        Object cachedCaptcha = captchaCache.getIfPresent(captchaKey);
        if (cachedCaptcha == null) {
            throw new BizException(ErrorCode.CAPTCHA_ERROR);
        }
        String cachedCode = cachedCaptcha instanceof String ? (String) cachedCaptcha : cachedCaptcha.toString();
        if (!cachedCode.equalsIgnoreCase(captchaCode)) {
            throw new BizException(ErrorCode.CAPTCHA_ERROR);
        }
        // 验证成功 → 消费图形验证码
        captchaCache.invalidate(captchaKey);
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
