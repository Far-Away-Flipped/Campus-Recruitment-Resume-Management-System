package com.atmoto.recruit.biz.common.domain;

import com.atmoto.recruit.common.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 教育经历（S-005，必填，支持多条）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stu_education")
public class StudentEducation extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private String schoolName;
    private String major;
    private String degree;
    private LocalDate startDate;
    private LocalDate endDate;
    @TableField("gpa_rank")
    private String gpa;
    private Integer sortOrder;
}
