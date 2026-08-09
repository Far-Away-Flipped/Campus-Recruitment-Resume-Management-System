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
 * 岗位主表（H-001）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("job_position")
public class JobPosition extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long jobId;

    private String title;

    /** 所属部门ID（DB列名为 department_id） */
    @TableField("department_id")
    private Long deptId;

    private Long categoryId;
    private String location;
    private String degreeRequirement;
    private String description;
    private String requirement;
    private String tags;
    private String status;
    private LocalDateTime deadline;

    /** 招聘人数（DB列名为 headcount，非 head_count） */
    @TableField("headcount")
    private Integer headcount;

    /** 排序号（DB列名 sort_order） */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 下线原因（DB列名 offline_reason） */
    @TableField("offline_reason")
    private String offlineReason;

    /** ★ 岗位负责人 HR 用户ID（数据范围权限锚点） */
    private Long ownerUserId;

    /** 投递数（DB列名为 apply_count） */
    @TableField("apply_count")
    private Integer applicationCount;

    private Integer viewCount;

    /** 部门名称（非DB字段，仅用于前端展示） */
    @TableField(exist = false)
    private String deptName;

    /** 岗位类别名称（仅前端展示，不存DB） */
    @TableField(exist = false)
    private String categoryName;
}
