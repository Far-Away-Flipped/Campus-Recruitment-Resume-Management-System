package com.atmoto.recruit.framework.security.context;

/**
 * HR后台 ThreadLocal —— 存放当前请求的HR用户ID与用户名
 * <p>用法：AdminUserHolder.getUserId() 获取当前HR用户ID，AdminUserHolder.clear() 清理</p>
 *
 * @author atmoto-recruit
 */
public class AdminUserHolder {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME_HOLDER = new ThreadLocal<>();

    public static void set(Long userId, String username) {
        USER_ID_HOLDER.set(userId);
        USERNAME_HOLDER.set(username);
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static String getUsername() {
        return USERNAME_HOLDER.get();
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
        USERNAME_HOLDER.remove();
    }
}
