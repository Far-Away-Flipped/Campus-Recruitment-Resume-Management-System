package com.atmoto.recruit.biz.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历列表项VO
 * <p>包含快照冗余列，用于列表展示</p>
 *
 * @author atmoto-recruit
 */
@Data
public class ResumeListVO {

    /** 投递记录ID */
    private Long applicationId;

    /** 学生ID */
    private Long studentId;

    /** 岗位ID */
    private Long jobId;

    /** 岗位名称 */
    private String jobTitle;

    /** 当前状态 */
    private String status;

    /** 状态中文名 */
    private String statusLabel;

    /** 学生姓名（快照冗余列） */
    private String studentName;

    /** 学校（快照冗余列） */
    private String school;

    /** 专业（快照冗余列） */
    private String major;

    /** 学历（快照冗余列） */
    private String degree;

    /** 投递时间 */
    private LocalDateTime applyTime;
}
