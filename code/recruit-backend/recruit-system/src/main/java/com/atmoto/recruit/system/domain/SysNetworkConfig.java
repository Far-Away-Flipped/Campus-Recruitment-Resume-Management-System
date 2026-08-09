package com.atmoto.recruit.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 网络配置开关（局域网访问开关等标量配置）
 * <p>对应表 sys_network_config，KV 模式，字段风格照抄 sys_brand_config（数据形状相同：一行一个配置项），
 * 但物理上是完全独立的表，且此表只挂 {@code /api/system/**} 下的认证接口，绝不出现在任何
 * {@code /api/portal/**} 匿名查询路径下。详见《HR 后台「网络管理」模块设计方案 V1.0》第 4.2.2 节。</p>
 */
@Data
@TableName("sys_network_config")
public class SysNetworkConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String configKey;
    private String configValue;
    /** 配置值类型：STRING/BOOLEAN/NUMBER（DB列名 config_type） */
    @TableField("config_type")
    private String configType;
    /** 配置分组（DB列名 config_group） */
    @TableField("config_group")
    private String configGroup;
    private String description;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
