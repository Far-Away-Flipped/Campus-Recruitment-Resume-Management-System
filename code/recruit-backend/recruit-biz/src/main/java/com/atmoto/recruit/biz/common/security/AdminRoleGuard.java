package com.atmoto.recruit.biz.common.security;

import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.atmoto.recruit.framework.security.context.AdminUserHolder;
import com.atmoto.recruit.system.service.ISysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * HR 角色守卫 —— 校验当前登录 HR 是否为「超级管理员」(admin) 角色
 * <p>安全边界在后端：前端 Sidebar 过滤/路由守卫仅为体验优化，可绕过，此处为兜底拦截。</p>
 */
@Component
@RequiredArgsConstructor
public class AdminRoleGuard {

    private final ISysRoleService sysRoleService;

    public void requireDirector() {
        Long userId = AdminUserHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        List<String> roleKeys = sysRoleService.selectRoleKeysByUserId(userId);
        if (!roleKeys.contains("admin")) {
            throw new BizException(ErrorCode.HR_DIRECTOR_ROLE_REQUIRED);
        }
    }
}
