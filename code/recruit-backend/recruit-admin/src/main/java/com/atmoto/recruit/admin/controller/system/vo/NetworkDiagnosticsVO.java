package com.atmoto.recruit.admin.controller.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 网络诊断响应 DTO（只读）
 * <p>
 * 【安全边界·强制】本类是显式构造的白名单式DTO，只包含以下明确列出的字段。绝不能直接序列化
 * Spring 的 {@code Environment}/{@code ServerProperties} 整个对象——那样会连带把
 * {@code jwt.admin.secret}、数据库连接串等敏感配置一并吐给前端。详见《HR 后台「网络管理」
 * 模块设计方案 V1.0》第 5.1/6 节【诊断接口信息泄露】防护条目。
 * </p>
 * <p>
 * 诊断内容是"被动推断当前请求是否经过预期的 Nginx 转发"，不做任何反向探测/出站请求
 * ——后端是被 Nginx 反代的一方，不应主动发起请求探测前置 Nginx 是否存活（方案 5.1 节）。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkDiagnosticsVO {

    /** 服务器监听地址，来自 ServerProperties.getAddress()（server.address 配置） */
    private String serverAddress;

    /** 服务器监听端口，来自 ServerProperties.getPort()（server.port 配置） */
    private Integer serverPort;

    /** 当前生效的CORS白名单Origin列表（复用 ICorsWhitelistService.getEffectiveRules()） */
    private List<String> corsAllowedOrigins;

    /** 局域网访问开关当前状态 */
    private boolean lanAccessEnabled;

    /** 本次请求的来源客户端IP（经 X-Forwarded-For/X-Real-IP 优先级处理后的结果） */
    private String requestClientIp;

    /** 本次请求的 Host 请求头原始值 */
    private String requestHost;

    /** X-Forwarded-For 请求头原始值，不存在则为null */
    private String forwardedForHeader;

    /** X-Forwarded-Proto 请求头原始值，不存在则为null */
    private String forwardedProtoHeader;

    /** 是否检测到Nginx反代痕迹（纯粹是"本次请求头是否存在Forwarded系列头"的字符串判断，不做出站探测） */
    private boolean nginxProxyDetected;
}
