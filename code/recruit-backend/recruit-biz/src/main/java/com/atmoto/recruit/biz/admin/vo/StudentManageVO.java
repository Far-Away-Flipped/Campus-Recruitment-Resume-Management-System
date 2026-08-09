package com.atmoto.recruit.biz.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学生用户管理列表VO
 * <p>用于管理后台学生用户列表展示，包含 stu_user 基本信息及投递数统计</p>
 *
 * @author atmoto-recruit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentManageVO {

    /** 学生ID */
    private Long studentId;

    /** 手机号 */
    private String phone;

    /** 姓名 */
    private String realName;

    /** 邮箱 */
    private String email;

    /** 注册时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 账号状态：ACTIVE/DISABLED/LOCKED */
    private String status;

    /** 投递数 */
    private Long applyCount;
}
