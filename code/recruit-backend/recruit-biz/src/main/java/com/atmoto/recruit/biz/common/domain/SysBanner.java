package com.atmoto.recruit.biz.common.domain;

import com.atmoto.recruit.common.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Banner 公告（A-001）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_banner")
public class SysBanner extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String content;
    private String imageUrl;
    private String linkUrl;
    private Integer sortOrder;
    /** DB列名 is_active */
    @TableField("is_active")
    private String status;

    /** Banner类型（DB列名 banner_type） */
    @TableField("banner_type")
    private String bannerType;

    /** 开始时间（DB列名 start_time） */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 结束时间（DB列名 end_time） */
    @TableField("end_time")
    private LocalDateTime endTime;
}
