package com.atmoto.recruit.framework.security.context;

/**
 * 学生端 ThreadLocal —— 存放当前请求的学生ID
 * <p>用法：PortalUserHolder.get() 获取当前学生ID，PortalUserHolder.clear() 清理</p>
 */
public class PortalUserHolder {

    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    public static void set(Long studentId) {
        HOLDER.set(studentId);
    }

    public static Long get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
