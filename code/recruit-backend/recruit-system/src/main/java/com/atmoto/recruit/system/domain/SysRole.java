package com.atmoto.recruit.system.domain;

import com.atmoto.recruit.common.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 系统角色 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long roleId;
    private String roleName;
    private String roleKey;
    private Integer roleSort;
    private String dataScope;
    private String status;
    private String remark;

    /** 菜单树严格模式（DB列名 menu_check_strictly） */
    @TableField("menu_check_strictly")
    private Integer menuCheckStrictly;

    /** 部门树严格模式（DB列名 dept_check_strictly） */
    @TableField("dept_check_strictly")
    private Integer deptCheckStrictly;
}
