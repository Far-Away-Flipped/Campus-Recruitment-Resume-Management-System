package com.atmoto.recruit.biz.portal.controller;

import com.atmoto.recruit.biz.portal.dto.*;
import com.atmoto.recruit.biz.portal.service.PortalAuthService;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.framework.security.context.PortalUserHolder;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 学生端认证 Controller
 * <p>前缀：/api/portal/auth，提供注册、登录、刷新Token、登出、重置密码、短信验证码等功能</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portal/auth")
public class PortalAuthController {

    private final PortalAuthService portalAuthService;

    @Qualifier("captchaCache")
    private final Cache<String, Object> captchaCache;

    @Qualifier("smsCodeCache")
    private final Cache<String, String> smsCodeCache;

    /**
     * 获取图形验证码
     * <p>返回 captchaKey + 验证码图片Base64</p>
     */
    @GetMapping("/captcha")
    public AjaxResult getCaptcha() throws Exception {
        String key = UUID.randomUUID().toString().substring(0, 8);
        String code = String.valueOf((int)(Math.random() * 9000) + 1000);
        captchaCache.put(key, code);
        log.info("验证码: key={}, code={}", key, code);

        // 生成图片
        int w = 120, h = 40;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        g.setColor(new java.awt.Color(245, 247, 250));
        g.fillRect(0, 0, w, h);
        for (int i = 0; i < 4; i++) {
            g.setColor(new java.awt.Color(200, 210, 220));
            g.drawLine((int)(Math.random()*w), (int)(Math.random()*h), (int)(Math.random()*w), (int)(Math.random()*h));
        }
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 24));
        for (int i = 0; i < 4; i++) {
            g.setColor(new java.awt.Color(30+(int)(Math.random()*80), 80+(int)(Math.random()*80), 140+(int)(Math.random()*80)));
            g.drawString(String.valueOf(code.charAt(i)), 20 + i * 25, 28 + (int)(Math.random() * 6));
        }
        g.dispose();

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "PNG", baos);
        String base64 = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(baos.toByteArray());

        return AjaxResult.success(Map.of("captchaKey", key, "captchaImage", base64));
    }

    /**
     * 发送短信验证码
     * <p>需先通过图形验证码校验（captchaKey + captchaCode），再由短信服务执行六层防刷</p>
     */
    @PostMapping("/sms-code")
    public AjaxResult sendSmsCode(@RequestBody SmsCodeRequest request) {
        String code = portalAuthService.sendSmsCode(request);
        return AjaxResult.success(Map.of("message", "验证码已发送", "code", code));
    }

    /**
     * 学生注册
     * <p>手机号+短信验证码+密码 → 创建账号与空资料 → 返回 Token</p>
     */
    @PostMapping("/register")
    public AjaxResult register(@RequestBody RegisterRequest request) {
        TokenResponse tokenResponse = portalAuthService.register(request);
        return AjaxResult.success("注册成功", tokenResponse);
    }

    /**
     * 学生登录
     * <p>手机号+密码 → 5次失败锁定15分钟 → 返回 Token</p>
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginRequest request) {
        TokenResponse tokenResponse = portalAuthService.login(request);
        return AjaxResult.success("登录成功", tokenResponse);
    }

    /**
     * 刷新 Token（轮换机制）
     * <p>旧 RefreshToken 标记为 rotated → 签发新的 accessToken + refreshToken</p>
     */
    @PostMapping("/refresh")
    public AjaxResult refresh(@RequestBody RefreshTokenRequest request) {
        TokenResponse tokenResponse = portalAuthService.refresh(request);
        return AjaxResult.success(tokenResponse);
    }

    /**
     * 登出
     * <p>吊销当前学生所有 RefreshToken（需登录态）</p>
     */
    @PostMapping("/logout")
    public AjaxResult logout() {
        Long studentId = PortalUserHolder.get();
        portalAuthService.logout(studentId);
        return AjaxResult.success("已登出");
    }

    /**
     * 重置密码
     * <p>手机号+短信验证码+新密码 → 更新密码并吊销所有 RefreshToken</p>
     */
    @PostMapping("/reset-password")
    public AjaxResult resetPassword(@RequestBody ResetPasswordRequest request) {
        portalAuthService.resetPassword(request);
        return AjaxResult.success("密码重置成功");
    }

    /**
     * 修改密码（登录后）
     * <p>校验旧密码 + 新密码强度 → 更新密码并吊销所有 RefreshToken</p>
     */
    @PostMapping("/change-password")
    public AjaxResult changePassword(@RequestBody ChangePasswordRequest request) {
        Long studentId = PortalUserHolder.get();
        portalAuthService.changePassword(request, studentId);
        return AjaxResult.success("密码修改成功");
    }
}
