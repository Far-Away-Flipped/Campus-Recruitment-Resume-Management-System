package com.atmoto.recruit.biz.common.domain;

import com.atmoto.recruit.common.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 投递记录（S-013，核心表）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_application")
public class Application extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long applicationId;

    private Long studentId;
    private Long jobId;
    private String status;
    /** 乐观锁版本号（DB列名 version_no） */
    @TableField("version_no")
    private Integer version;
    /** 当前快照ID指针 */
    private Long currentSnapshotId;
    /** 渠道来源 */
    private String source;
    /** 渠道来源详情（DB列名 source_detail） */
    @TableField("source_detail")
    private String sourceDetail;
    /** 渠道来源中文快照（投递时按字典固化，DB列名 source_label） */
    @TableField("source_label")
    private String sourceLabel;
    private LocalDateTime applyTime;

    // ── C-06 裁决：筛选字段冗余为普通列（DB中已实际存在） ──
    private String snapshotSchool;
    private String snapshotMajor;
    private String snapshotDegree;
    private String snapshotName;

    /** 允许撤回重投（P2） */
    private String allowResubmit;

    /** 数据保留天数（DB列名 data_retention_days） */
    @TableField("data_retention_days")
    private Integer dataRetentionDays;

    /** 自动清理日期（DB列名 auto_cleanup_date） */
    @TableField("auto_cleanup_date")
    private LocalDate autoCleanupDate;
}
