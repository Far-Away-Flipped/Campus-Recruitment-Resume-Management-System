package com.atmoto.recruit.biz.common.domain;

import com.atmoto.recruit.common.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 学生基本资料（S-004）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stu_profile")
public class StudentProfile extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    @TableField("real_name")
    private String name;
    private String gender;
    private LocalDate birthDate;
    private String phone;
    private String email;
    @TableField("current_residence")
    private String currentCity;
    @TableField("native_place")
    private String nativePlace;

    @TableField("avatar_url")
    private String avatarUrl;

    /** 备注（仅前端使用，不存DB） */
    @TableField(exist = false)
    private String remark;
}
