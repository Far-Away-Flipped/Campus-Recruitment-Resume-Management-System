package com.atmoto.recruit.admin.controller;

import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.atmoto.recruit.framework.security.context.AdminUserHolder;
import com.atmoto.recruit.framework.web.service.AdminTokenService;
import com.atmoto.recruit.system.domain.SysUser;
import com.atmoto.recruit.system.mapper.SysUserMapper;
import com.atmoto.recruit.system.service.ISysRoleService;
import com.atmoto.recruit.system.service.ISysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
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
    private final ISysRoleService sysRoleService;
    private final ISysUserService sysUserService;
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
        SysUser user = sysUserMapper.selectById(userId);
        java.util.List<String> roleKeys = sysRoleService.selectRoleKeysByUserId(userId);
        boolean isSuperAdmin = roleKeys.contains("admin");
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("userId", userId);
        data.put("userName", username);
        data.put("nickName", user != null ? user.getNickName() : null);
        data.put("email", user != null ? user.getEmail() : null);
        data.put("phonenumber", user != null ? user.getPhonenumber() : null);
        data.put("sex", user != null ? user.getSex() : null);
        data.put("roleKeys", roleKeys);
        data.put("isSuperAdmin", isSuperAdmin);
        return AjaxResult.success(data);
    }

    /** 更新本人信息（白名单字段：nickName/email/phonenumber/sex） */
    @PutMapping("/profile")
    public AjaxResult updateProfile(@RequestBody Map<String, String> body) {
        Long userId = AdminUserHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        String nickName = body.get("nickName");
        String email = body.get("email");
        String phonenumber = body.get("phonenumber");
        String sex = body.get("sex");

        // 唯一性校验（编辑时排除自身）
        SysUser checkUser = new SysUser();
        checkUser.setUserId(userId);
        if (StringUtils.hasText(email)) {
            checkUser.setEmail(email);
            if (!sysUserService.checkEmailUnique(checkUser)) {
                throw new BizException(ErrorCode.PARAM_INVALID, "邮箱已存在");
            }
        }
        if (StringUtils.hasText(phonenumber)) {
            checkUser.setPhonenumber(phonenumber);
            if (!sysUserService.checkPhoneUnique(checkUser)) {
                throw new BizException(ErrorCode.PARAM_INVALID, "手机号码已存在");
            }
        }

        // 仅更新白名单字段（userId 定位记录，其余字段按非空更新策略，null 不覆盖）
        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setNickName(nickName);
        update.setEmail(email);
        update.setPhonenumber(phonenumber);
        update.setSex(sex);
        sysUserService.updateUser(update);

        return AjaxResult.success("个人信息更新成功");
    }

    /** 修改本人密码（校验旧密码 + 新密码强度，成功后吊销当前 token） */
    @PutMapping("/password")
    public AjaxResult updatePassword(@RequestBody Map<String, String> body,
                                     HttpServletRequest request) {
        Long userId = AdminUserHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "原密码和新密码不能为空");
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        // 校验旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException(ErrorCode.OLD_PASSWORD_INCORRECT);
        }

        // 新密码强度：至少8位，且同时包含字母和数字
        if (newPassword.length() < 8 || !newPassword.matches(".*[a-zA-Z].*") || !newPassword.matches(".*\\d.*")) {
            throw new BizException(ErrorCode.PASSWORD_TOO_WEAK);
        }

        // 仅更新 password 字段（复用 resetPwd 的精确更新逻辑）
        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setPassword(passwordEncoder.encode(newPassword));
        sysUserService.resetPwd(update);

        // 改密码后吊销当前 token（复用 AdminTokenService.revokeToken）
        String token = extractToken(request);
        if (StringUtils.hasText(token)) {
            adminTokenService.revokeToken(token);
        }

        return AjaxResult.success("密码修改成功");
    }

    /** 从 Authorization 头提取 Bearer Token（与 AdminTokenFilter 保持一致） */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
