package com.atmoto.recruit.biz.common.domain;

import com.atmoto.recruit.common.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生账号（独立于 sys_user）
 * <p>JWT 签发使用本表的 studentId，不入 RuoYi 体系</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stu_user")
public class Student extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long studentId;

    /** 手机号（唯一索引，登录凭据） */
    private String phone;

    /** 真实姓名（DB列名 real_name） */
    private String realName;

    /** 邮箱 */
    private String email;

    /** BCrypt 密码哈希 */
    private String passwordHash;

    /** 账号状态：ACTIVE/DISABLED/LOCKED */
    private String status;

    private String lastLoginIp;
    private LocalDateTime lastLoginTime;
    private Integer loginFailCount;
    private LocalDateTime lockUntil;

    /** 数据保留天数（已确认：未录用365、已淘汰180） */
    private Integer dataRetentionDays;
    private LocalDate autoCleanupDate;

    /** 隐私政策同意 */
    private String privacyAgreed;
    private LocalDateTime privacyAgreedTime;
}
