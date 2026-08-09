package com.atmoto.recruit.biz.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 学生详情VO
 * <p>用于管理后台学生详情页展示，包含学生基本信息及各维度子列表</p>
 *
 * @author atmoto-recruit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDetailVO {

    /** 学生ID */
    private Long studentId;

    /** 手机号（完整，不脱敏） */
    private String phone;

    /** 真实姓名 */
    private String realName;

    /** 邮箱 */
    private String email;

    /** 性别：M-男 / F-女 / O-其他 */
    private String gender;

    /** 出生日期 */
    private String birthDate;

    /** 当前所在城市 */
    private String currentCity;

    /** 头像URL */
    private String avatarUrl;

    /** 账号状态 */
    private String status;

    /** 账号状态中文标签 */
    private String statusLabel;

    /** 注册时间 */
    private String createTime;

    // ── 子列表 ──

    /** 教育经历列表 */
    private List<EducationVO> educations;

    /** 实习/项目经历列表 */
    private List<InternshipBriefVO> internships;

    /** 简历附件列表 */
    private List<ResumeFileBriefVO> resumeFiles;

    /** 投递历史列表 */
    private List<ApplicationBriefVO> applications;
}
