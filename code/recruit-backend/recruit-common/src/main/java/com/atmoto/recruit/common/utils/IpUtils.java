package com.atmoto.recruit.common.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端真实IP提取工具
 * <p>
 * 提取自 {@code PortalAuthServiceImpl.getClientIp()} / {@code SmsCodeServiceImpl.getClientIp()}
 * 中原本重复的两份实现（逻辑完全一致：优先 X-Forwarded-For，其次 X-Real-IP，最后回退
 * {@code request.getRemoteAddr()}）。本次网络管理模块（诊断接口 + 审计埋点）需要同样的IP
 * 提取逻辑，为避免第三次复制粘贴，下沉为 recruit-common 的共享工具类。
 * </p>
 * <p>
 * 【注意】原两处实现暂未同步改造为调用本工具类（超出本次网络管理模块范围，不做无关改动），
 * 新增代码统一调用本类，避免出现第三份重复实现。
 * </p>
 */
public final class IpUtils {

    private IpUtils() {
    }

    /**
     * 从请求中提取客户端真实IP
     * <p>优先级：X-Forwarded-For（取第一个，即最原始客户端IP）→ X-Real-IP → request.getRemoteAddr()</p>
     *
     * @param request 当前HTTP请求，不能为null
     * @return 客户端IP；异常或无法获取时返回 "unknown"
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        try {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            // X-Forwarded-For 可能是逗号分隔的多级代理链，第一个是最初的客户端IP
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
