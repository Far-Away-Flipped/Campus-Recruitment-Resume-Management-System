package com.atmoto.recruit.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 网络配置变更审计记录
 * <p>对应表 {@code audit_network_config}，不可变、仅追加日志（无 update_time/del_flag，
 * 因此不继承 BaseEntity）。记录 CORS 白名单与局域网开关的每一次写操作：谁在什么时间
 * 把什么值改成了什么值。详见《HR 后台「网络管理」模块设计方案 V1.0》第 4.2.3 节。</p>
 */
@Data
@TableName("audit_network_config")
public class NetworkConfigLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联规则ID（规则被删除后此字段仍保留历史值；局域网开关等非规则类配置为null） */
    private Long ruleId;

    /** 被修改的配置来源：CORS_ORIGIN-白名单条目, NETWORK_CONFIG-开关配置 */
    private String configTable;

    /** 操作类型：ADD/UPDATE/DELETE/ENABLE/DISABLE/TOGGLE */
    private String operationType;

    /** 变更前的值（JSON快照或简单值） */
    private String oldValue;

    /** 变更后的值 */
    private String newValue;

    /** 操作详情补充 */
    private String operationDetail;

    /** 操作人ID（关联sys_user.user_id） */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 操作来源IP地址 */
    private String ipAddress;

    /** 浏览器User-Agent */
    private String userAgent;

    private LocalDateTime createTime;
}
