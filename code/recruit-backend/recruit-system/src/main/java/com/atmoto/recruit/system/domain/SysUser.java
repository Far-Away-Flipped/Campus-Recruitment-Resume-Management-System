package com.atmoto.recruit.system.domain;

import com.atmoto.recruit.common.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统用户（HR 与管理员）
 * 注意事项：学生有独立的 stu_user 表，不使用本表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long userId;
    private Long deptId;
    private String userName;
    private String nickName;
    private String userType;
    private String email;
    private String phonenumber;
    private String sex;
    private String avatar;
    @JsonIgnore
    private String password;
    private String status;
    private String loginIp;
    private LocalDateTime loginDate;
    /** 备注 */
    private String remark;

    /** 用户关联的角色ID列表（非表字段，仅用于角色回填/持久化） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private java.util.List<Long> roleIds;

    /** 用户关联的角色名称列表（非表字段，仅用于角色回填展示） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private java.util.List<String> roleNames;
}
