package com.atmoto.recruit.admin.controller;

import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.atmoto.recruit.framework.security.context.AdminUserHolder;
import com.atmoto.recruit.framework.web.service.AdminTokenService;
import com.atmoto.recruit.system.domain.SysUser;
import com.atmoto.recruit.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

/**
 * HR 后台认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminTokenService adminTokenService;

    @Qualifier("captchaCache")
    private final Cache<String, Object> captchaCache;

    @Qualifier("loginFailCache")
    private final Cache<String, Integer> loginFailCache;

    /** 获取图形验证码图片 */
    @GetMapping("/captcha")
    public void getCaptchaImage(@RequestParam("key") String key,
                                 HttpServletResponse response) throws Exception {
        // 生成4位随机验证码
        String code = String.valueOf((int)(Math.random() * 9000) + 1000);
        captchaCache.put("captcha_codes:" + key, code);
        log.debug("验证码: key={}, code={}", key, code);

        // 生成图片
        int w = 120, h = 40;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        // 背景
        g.setColor(new java.awt.Color(245, 247, 250));
        g.fillRect(0, 0, w, h);
        // 干扰线
        g.setColor(new java.awt.Color(200, 210, 220));
        for (int i = 0; i < 4; i++) {
            g.drawLine((int)(Math.random()*w), (int)(Math.random()*h),
                       (int)(Math.random()*w), (int)(Math.random()*h));
        }
        // 文字
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 24));
        for (int i = 0; i < 4; i++) {
            g.setColor(new java.awt.Color(30 + (int)(Math.random()*80), 80 + (int)(Math.random()*80), 140 + (int)(Math.random()*80)));
            g.drawString(String.valueOf(code.charAt(i)), 20 + i * 25, 28 + (int)(Math.random() * 6));
        }
        g.dispose();

        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-cache");
        javax.imageio.ImageIO.write(image, "PNG", response.getOutputStream());
    }

    /** HR 登录 */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String captchaKey = body.get("captchaKey");
        String captchaCode = body.get("captchaCode");

        // 校验图形验证码（同一key最多尝试3次，防暴力破解）
        String cachedCode = (String) captchaCache.getIfPresent("captcha_codes:" + captchaKey);
        if (cachedCode == null || !cachedCode.equalsIgnoreCase(captchaCode)) {
            // 记录失败次数，超过3次则失效该验证码
            String failKey = "captcha_fail:" + captchaKey;
            Integer failCount = (Integer) captchaCache.getIfPresent(failKey);
            if (failCount == null) failCount = 0;
            failCount++;
            if (failCount >= 3) {
                captchaCache.invalidate("captcha_codes:" + captchaKey);
                captchaCache.invalidate(failKey);
            } else {
                captchaCache.put(failKey, failCount);
            }
            throw new BizException(ErrorCode.CAPTCHA_ERROR);
        }
        captchaCache.invalidate("captcha_codes:" + captchaKey); // 验证后消费

        // 查询用户
        SysUser user = sysUserMapper.selectOne(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUserName, username)
        );
        if (user == null) {
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }

        // 检查账号状态：已停用用户拒绝登录
        if ("1".equals(user.getStatus())) {
            log.warn("已停用账号尝试登录：username={}", username);
            throw new BizException(ErrorCode.USER_DISABLED);
        }

        // 检查锁定状态：连续5次失败则锁定
        Integer failCount = loginFailCache.getIfPresent("login_fail:" + username);
        if (failCount != null && failCount >= 5) {
            throw new BizException(ErrorCode.ACCOUNT_LOCKED);
        }

        // 验密
        if (!passwordEncoder.matches(password, user.getPassword())) {
            int newCount = (failCount != null ? failCount : 0) + 1;
            loginFailCache.put("login_fail:" + username, newCount);
            log.warn("HR登录失败：username={}, 第{}次", username, newCount);
            // 第5次失败即触发锁定，而非等到第6次请求时才拦截
            if (newCount >= 5) {
                throw new BizException(ErrorCode.ACCOUNT_LOCKED);
            }
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }

        // 成功：清除失败计数
        loginFailCache.invalidate("login_fail:" + username);

        // 签发 Token
        String token = adminTokenService.createToken(user.getUserId(), user.getUserName());

        return AjaxResult.success(Map.of(
            "token", token,
            "userId", user.getUserId(),
            "userName", user.getUserName()
        ));
    }

    /** 获取当前用户信息 */
    @GetMapping("/info")
    public AjaxResult getUserInfo() {
        Long userId = AdminUserHolder.getUserId();
        String username = AdminUserHolder.getUsername();
        if (userId == null) {
            return AjaxResult.error(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMsg());
        }
        return AjaxResult.success(Map.of("userId", userId, "userName", username));
    }
}
