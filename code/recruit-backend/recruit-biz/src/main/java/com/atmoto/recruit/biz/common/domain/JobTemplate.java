package com.atmoto.recruit.biz.common.domain;

import com.atmoto.recruit.common.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位模板（H-003）
 * <p>预置岗位模板，HR可使用模板快速创建岗位，预填岗位名称、部门、类别、地点、学历要求、职责描述、任职要求、标签等</p>
 *
 * @author atmoto-recruit
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("job_template")
public class JobTemplate extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板名称（DB列名 template_name） */
    @TableField("template_name")
    private String templateName;

    /** 岗位名称（DB列名 title） */
    private String title;

    /** 所属部门ID（DB列名 department_id） */
    @TableField("department_id")
    private Long deptId;

    /** 岗位类别ID（DB列名 category_id） */
    @TableField("category_id")
    private Long categoryId;

    /** 工作地点 */
    private String location;

    /** 学历要求（DB列名 degree_requirement） */
    @TableField("degree_requirement")
    private String degreeRequirement;

    /** 招聘人数 */
    private Integer headcount;

    /** 岗位职责描述 */
    private String description;

    /** 任职要求 */
    private String requirement;

    /** 岗位标签（JSON数组） */
    private String tags;
}
