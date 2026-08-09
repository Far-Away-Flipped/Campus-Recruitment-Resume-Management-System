package com.atmoto.recruit.biz.common.domain;

import com.atmoto.recruit.common.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位类别（A-002）
 * <p>三大序列：硬件研发 / 软件研发 / 职能</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("job_category")
public class JobCategory extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long categoryId;

    private Long parentId;
    private String ancestors;
    private String categoryName;
    private String categoryCode;
    @TableField("sort_order")
    private Integer orderNum;
    private String status;
}
