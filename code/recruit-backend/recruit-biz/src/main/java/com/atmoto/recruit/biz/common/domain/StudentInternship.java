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
 * 学生实习/项目经历（S-006）
 * <p>recordType: I-实习经历, P-项目经历</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stu_internship")
public class StudentInternship extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    @TableField("record_type")
    private String recordType;

    @TableField("org_name")
    private String orgName;

    private String position;

    private LocalDate startDate;

    private LocalDate endDate;

    private String description;

    private Integer sortOrder;
}
