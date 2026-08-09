package com.atmoto.recruit.biz.common.domain;

import com.atmoto.recruit.common.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学生社团经历（S-009）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stu_activity")
public class StudentActivity extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    @TableField("org_name")
    private String orgName;

    private String position;

    private String description;

    private Integer sortOrder;
}
