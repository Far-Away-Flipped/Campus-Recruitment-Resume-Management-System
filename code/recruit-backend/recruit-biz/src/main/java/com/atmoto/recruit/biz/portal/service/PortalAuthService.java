package com.atmoto.recruit.biz.portal.service;

import com.atmoto.recruit.biz.portal.dto.*;

/**
 * 学生端认证服务接口
 * <p>封装注册、登录、刷新Token、登出、重置密码等认证逻辑</p>
 */
public interface PortalAuthService {

    /**
     * 发送短信验证码
     * <p>包含图形验证码前置校验 + 六层防刷</p>
     *
     * @param request 请求（手机号 + 图形验证码）
     * @return 生成的验证码（开发环境可返回给前端）
     */
    String sendSmsCode(SmsCodeRequest request);

    /**
     * 学生注册
     *
     * @param request 注册请求（手机号 + 验证码 + 密码）
     * @return Token响应（accessToken + refreshToken）
     */
    TokenResponse register(RegisterRequest request);

    /**
     * 学生登录
     *
     * @param request 登录请求（手机号 + 密码）
     * @return Token响应（accessToken + refreshToken）
     */
    TokenResponse login(LoginRequest request);

    /**
     * 刷新 Token（轮换机制）
     * <p>旧 RefreshToken 标记为 rotated，签发新的 accessToken + refreshToken</p>
     *
     * @param request 刷新请求
     * @return 新的Token响应
     */
    TokenResponse refresh(RefreshTokenRequest request);

    /**
     * 登出（吊销 RefreshToken）
     *
     * @param studentId 学生ID
     */
    void logout(Long studentId);

    /**
     * 重置密码
     *
     * @param request 重置密码请求（手机号 + 验证码 + 新密码）
     */
    void resetPassword(ResetPasswordRequest request);

    /**
     * 修改密码（登录后）
     * <p>校验旧密码 + 新密码强度，成功后吊销该学生所有 RefreshToken</p>
     *
     * @param request   修改密码请求（旧密码 + 新密码）
     * @param studentId 学生ID（从登录态获取，绝不来自请求参数）
     */
    void changePassword(ChangePasswordRequest request, Long studentId);
}
