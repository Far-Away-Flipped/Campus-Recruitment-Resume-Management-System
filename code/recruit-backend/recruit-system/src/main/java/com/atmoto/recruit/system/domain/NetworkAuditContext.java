package com.atmoto.recruit.system.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网络配置写操作的审计上下文
 * <p>
 * 承载"是谁、从哪个IP、用什么浏览器"发起了本次写操作。之所以由 Controller 层构造后
 * 传入 Service，而不是 Service 直接读取 {@code AdminUserHolder}/{@code HttpServletRequest}：
 * {@code AdminUserHolder} 定义在 recruit-framework 模块，而 recruit-system 依赖方向是
 * "recruit-framework 依赖 recruit-system"（详见方案 3.2 节裁决1），recruit-system 反过来
 * 引用 recruit-framework 的类会构成环形依赖，Maven reactor 编译期直接报错。
 * Controller（recruit-admin 模块，同时依赖 framework 和 system）持有 AdminUserHolder
 * 与 HttpServletRequest 的访问权限，因此由 Controller 组装本上下文对象传入 Service。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkAuditContext {

    /** 操作人ID，来自 AdminUserHolder.getUserId() */
    private Long operatorId;

    /** 操作人姓名，来自 AdminUserHolder.getUsername() */
    private String operatorName;

    /** 操作来源IP，来自 IpUtils.getClientIp(request) */
    private String ipAddress;

    /** 浏览器User-Agent，来自 request.getHeader("User-Agent") */
    private String userAgent;
}
