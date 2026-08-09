package com.atmoto.recruit.biz.portal.service.impl;

import com.atmoto.recruit.biz.common.domain.Student;
import com.atmoto.recruit.biz.common.domain.StudentProfile;
import com.atmoto.recruit.biz.common.domain.StudentRefreshToken;
import com.atmoto.recruit.biz.common.mapper.StudentMapper;
import com.atmoto.recruit.biz.common.mapper.StudentProfileMapper;
import com.atmoto.recruit.biz.common.mapper.StudentRefreshTokenMapper;
import com.atmoto.recruit.biz.portal.dto.*;
import com.atmoto.recruit.biz.portal.service.PortalAuthService;
import com.atmoto.recruit.biz.sms.SmsCodeService;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.atmoto.recruit.framework.web.service.PortalTokenService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 学生端认证服务实现
 * <p>
 * 核心功能：注册、登录（含失败锁定）、Token轮换刷新、登出吊销、重置密码
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalAuthServiceImpl implements PortalAuthService {

    private final StudentMapper studentMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final StudentRefreshTokenMapper refreshTokenMapper;
    private final SmsCodeService smsCodeService;
    private final PortalTokenService portalTokenService;
    private final PasswordEncoder passwordEncoder;

    @Qualifier("loginFailCache")
    private final Cache<String, Integer> loginFailCache;

    @Value("${jwt.portal.secret}")
    private String portalSecret;

    /** 登录失败锁定阈值 */
    private static final int MAX_LOGIN_FAIL = 5;

    /** 登录失败锁定时间（分钟） */
    private static final int LOCK_DURATION_MINUTES = 15;

    @Override
    public String sendSmsCode(SmsCodeRequest request) {
        // 校验请求参数非空
        if (request.getCaptchaKey() == null || request.getCaptchaCode() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "图形验证码不能为空");
        }

        // 校验图形验证码（必须成功）
        smsCodeService.verifyCaptcha(request.getCaptchaKey(), request.getCaptchaCode());

        // 发送短信验证码
        return smsCodeService.sendCode(request.getPhone());
    }

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String phone = request.getPhone();
        String password = request.getPassword();

        // 0. 校验隐私协议同意
        if (!Boolean.TRUE.equals(request.getPrivacyAgreed())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请先同意隐私协议");
        }

        // 0a. 手机号格式校验
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException(ErrorCode.PARAM_INVALID, "手机号格式不正确");
        }

        // 0b. 密码强度校验
        if (password == null || password.length() < 8) {
            throw new BizException(ErrorCode.PARAM_INVALID, "密码长度不能少于8位");
        }

        // 1. 校验短信验证码
        if (!smsCodeService.verifyCode(phone, request.getSmsCode())) {
            throw new BizException(ErrorCode.CAPTCHA_ERROR);
        }

        // 2. 校验手机号唯一性
        Long existingCount = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>().eq(Student::getPhone, phone));
        if (existingCount > 0) {
            throw new BizException(ErrorCode.STUDENT_PHONE_EXISTS);
        }

        // 3. BCrypt 加密密码
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 4. 插入 stu_user
        Student student = new Student();
        student.setPhone(phone);
        student.setPasswordHash(hashedPassword);
        student.setStatus("ACTIVE");
        student.setDataRetentionDays(365);
        student.setAutoCleanupDate(LocalDate.now().plusDays(365));
        student.setLoginFailCount(0);
        // 隐私政策同意记录
        student.setPrivacyAgreed("1");
        student.setPrivacyAgreedTime(LocalDateTime.now());
        studentMapper.insert(student);

        // 5. 插入 stu_profile 空记录（仅 studentId + phone）
        StudentProfile profile = new StudentProfile();
        profile.setStudentId(student.getStudentId());
        profile.setPhone(phone);
        studentProfileMapper.insert(profile);

        log.info("学生注册成功：studentId={}, phone={}", student.getStudentId(), phone);

        // 6. 签发并返回 Token
        return issueTokens(student.getStudentId(), phone);
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        String phone = request.getPhone();

        // 1. 查询学生账号
        Student student = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>().eq(Student::getPhone, phone));
        if (student == null) {
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }

        // 2. 校验账号状态
        if ("DISABLED".equals(student.getStatus())) {
            throw new BizException(ErrorCode.STUDENT_ACCOUNT_DISABLED);
        }

        // 3. 检查是否在锁定期内
        if (student.getLockUntil() != null && student.getLockUntil().isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.ACCOUNT_LOCKED);
        }

        // 4. BCrypt 验密
        if (!passwordEncoder.matches(request.getPassword(), student.getPasswordHash())) {
            // 密码错误 → 递增失败计数
            handleLoginFail(phone, student);
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }

        // 5. 登录成功 → 清零失败计数，更新登录时间与IP
        student.setLoginFailCount(0);
        student.setLockUntil(null);
        student.setLastLoginTime(LocalDateTime.now());
        student.setLastLoginIp(getClientIp());
        studentMapper.updateById(student);

        // 清除登录失败缓存
        loginFailCache.invalidate("login_fail:" + phone);

        log.info("学生登录成功：studentId={}, phone={}", student.getStudentId(), phone);

        // 6. 签发 Token
        return issueTokens(student.getStudentId(), phone);
    }

    @Override
    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        String rawRefreshToken = request.getRefreshToken();
        String tokenHash = sha256(rawRefreshToken);

        // 0. 先查询该 token hash 是否存在于数据库（不限状态），检测 token 重用（盗用）场景
        StudentRefreshToken anyToken = refreshTokenMapper.selectOne(
                new LambdaQueryWrapper<StudentRefreshToken>()
                        .eq(StudentRefreshToken::getTokenHash, tokenHash));
        if (anyToken != null && "ROTATED".equals(anyToken.getStatus())) {
            // Token 已被轮换过又被使用，说明是重用（可能是盗用），全链吊销该学生所有 refresh token
            log.warn("检测到 RefreshToken 重用（盗用风险）：studentId={}, tokenId={}",
                    anyToken.getStudentId(), anyToken.getId());
            var activeTokens = refreshTokenMapper.selectList(
                    new LambdaQueryWrapper<StudentRefreshToken>()
                            .eq(StudentRefreshToken::getStudentId, anyToken.getStudentId())
                            .eq(StudentRefreshToken::getStatus, "ACTIVE"));
            for (StudentRefreshToken token : activeTokens) {
                token.setStatus("REVOKED");
                refreshTokenMapper.updateById(token);
            }
            log.info("因 Token 重用已全链吊销 {} 个 refresh token：studentId={}",
                    activeTokens.size(), anyToken.getStudentId());
            throw new BizException(ErrorCode.TOKEN_REUSE_DETECTED);
        }

        // 1. 查库验证 RefreshToken 存在且有效
        StudentRefreshToken storedToken = refreshTokenMapper.selectOne(
                new LambdaQueryWrapper<StudentRefreshToken>()
                        .eq(StudentRefreshToken::getTokenHash, tokenHash)
                        .eq(StudentRefreshToken::getStatus, "ACTIVE"));
        if (storedToken == null) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }

        // 2. 检查是否已过期
        if (storedToken.getExpireTime().isBefore(LocalDateTime.now())) {
            storedToken.setStatus("EXPIRED");
            refreshTokenMapper.updateById(storedToken);
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }

        // 3. 验证 JWT 签名与过期时间，提取 studentId
        Long studentId;
        try {
            SecretKey key = new SecretKeySpec(
                    Base64.getDecoder().decode(portalSecret.getBytes(StandardCharsets.UTF_8)),
                    "HmacSHA256");
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(rawRefreshToken)
                    .getPayload();
            studentId = Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            // JWT 解析失败 → 标记 token 为无效
            storedToken.setStatus("INVALID");
            refreshTokenMapper.updateById(storedToken);
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }

        // 4. 轮换：标记旧 Token 为 rotated
        storedToken.setStatus("ROTATED");
        storedToken.setRotatedAt(LocalDateTime.now());
        refreshTokenMapper.updateById(storedToken);

        // 从数据库获取 phone（不在 JWT 中存储 PII）
        Student stu = studentMapper.selectById(studentId);
        String phone = stu != null ? stu.getPhone() : "";

        log.info("RefreshToken 轮换：studentId={}, oldTokenId={}", studentId, storedToken.getId());

        // 5. 签发新 Token
        return issueTokens(studentId, phone);
    }

    @Override
    @Transactional
    public void logout(Long studentId) {
        // 将该学生所有 ACTIVE 状态的 RefreshToken 标记为 REVOKED
        var tokens = refreshTokenMapper.selectList(
                new LambdaQueryWrapper<StudentRefreshToken>()
                        .eq(StudentRefreshToken::getStudentId, studentId)
                        .eq(StudentRefreshToken::getStatus, "ACTIVE"));
        for (StudentRefreshToken token : tokens) {
            token.setStatus("REVOKED");
            refreshTokenMapper.updateById(token);
        }
        log.info("学生登出，已吊销 {} 个 RefreshToken：studentId={}", tokens.size(), studentId);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String phone = request.getPhone();

        // 1. 校验短信验证码
        if (!smsCodeService.verifyCode(phone, request.getSmsCode())) {
            throw new BizException(ErrorCode.CAPTCHA_ERROR);
        }

        // 2. 查找学生账号（不泄露账号是否存在）
        Student student = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>().eq(Student::getPhone, phone));
        if (student == null) {
            log.info("密码重置请求：手机号 {} 未注册，统一返回成功防止账号枚举", phone);
            return;
        }

        // 3. BCrypt 加密新密码并更新
        student.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        studentMapper.updateById(student);

        // 4. 重置密码后吊销所有 RefreshToken（安全措施）
        var tokens = refreshTokenMapper.selectList(
                new LambdaQueryWrapper<StudentRefreshToken>()
                        .eq(StudentRefreshToken::getStudentId, student.getStudentId())
                        .eq(StudentRefreshToken::getStatus, "ACTIVE"));
        for (StudentRefreshToken token : tokens) {
            token.setStatus("REVOKED");
            refreshTokenMapper.updateById(token);
        }

        log.info("密码重置成功：studentId={}, phone={}, 已吊销 {} 个Token",
                student.getStudentId(), phone, tokens.size());
    }

    // ────────────────── 内部辅助方法 ──────────────────

    /**
     * 签发 accessToken + refreshToken（并落库 refreshToken）
     */
    private TokenResponse issueTokens(Long studentId, String phone) {
        String accessToken = portalTokenService.createAccessToken(studentId, phone);
        String refreshToken = portalTokenService.createRefreshToken(studentId, phone);

        // refreshToken 落库
        StudentRefreshToken tokenEntity = new StudentRefreshToken();
        tokenEntity.setStudentId(studentId);
        tokenEntity.setTokenHash(sha256(refreshToken));
        tokenEntity.setDeviceInfo(getClientIp());
        tokenEntity.setExpireTime(LocalDateTime.now().plusDays(7));
        tokenEntity.setStatus("ACTIVE");
        tokenEntity.setCreateTime(LocalDateTime.now());
        refreshTokenMapper.insert(tokenEntity);

        return TokenResponse.of(accessToken, refreshToken);
    }

    /**
     * 处理登录失败：递增失败计数，5次后锁定15分钟
     */
    private void handleLoginFail(String phone, Student student) {
        int failCount = student.getLoginFailCount() != null ? student.getLoginFailCount() + 1 : 1;
        student.setLoginFailCount(failCount);

        if (failCount >= MAX_LOGIN_FAIL) {
            // 锁定15分钟
            student.setLockUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            log.warn("账号登录失败达 {} 次，已锁定至 {}：phone={}", failCount, student.getLockUntil(), phone);
        }

        studentMapper.updateById(student);
    }

    /**
     * SHA-256 哈希（用于 RefreshToken 安全存储）
     */
    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }

    /**
     * 获取客户端真实IP
     */
    private static String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "unknown";
            }
            var request = attrs.getRequest();
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
