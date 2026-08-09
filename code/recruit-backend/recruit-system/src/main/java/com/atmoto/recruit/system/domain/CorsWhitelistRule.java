package com.atmoto.recruit.system.domain;

import com.atmoto.recruit.common.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * CORS 动态白名单规则
 * <p>对应表 sys_cors_origin，Origin 精确匹配（EXACT）与局域网网段匹配（CIDR）统一存储，
 * rule_type 判别列区分类型。详见《HR 后台「网络管理」模块设计方案 V1.0》第 3.4/4.2.1 节。</p>
 * <p>
 * 注意：sys_cors_origin 表未设计 del_flag 列（方案 DDL 原文如此，白名单规则通过 is_active
 * 软启停而非逻辑删除，物理删除仅在 is_builtin=0 时允许），因此排除 BaseEntity 继承来的
 * delFlag 字段的 SQL 映射，避免 MyBatis-Plus 全局逻辑删除插件拼接不存在的列导致 SQL 报错。
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_cors_origin", excludeProperty = "delFlag")
public class CorsWhitelistRule extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则类型：EXACT-精确Origin匹配, CIDR-局域网网段匹配 */
    private String ruleType;

    /** EXACT时为完整Origin(scheme://host:port)；CIDR时为网段(如192.168.31.0/24) */
    private String ruleValue;

    /** 规则说明 */
    private String description;

    /** 启用标记：1-启用 0-停用（停用不删除，保留历史） */
    private String isActive;

    /** 是否内置默认规则：1-是（拒绝物理删除，只可禁用） */
    private String isBuiltin;
}
