package com.atmoto.recruit.system.domain;

import com.atmoto.recruit.common.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 字典类型 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long dictId;
    private String dictName;
    private String dictType;
    private String status;
    private String remark;
}