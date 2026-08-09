package com.atmoto.recruit.biz.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 品牌配置（A-001）
 */
@Data
@TableName("sys_brand_config")
public class BrandConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String configKey;
    private String configValue;
    private String description;
    /** 配置类型（DB列名 config_type） */
    @TableField("config_type")
    private String configType;
    /** 配置分组（DB列名 config_group） */
    @TableField("config_group")
    private String configGroup;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
