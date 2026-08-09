package com.atmoto.recruit.biz.portal.vo;

import com.atmoto.recruit.biz.common.domain.AppStatusHistory;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 投递详情 VO（S-013 投递详情含状态流转历史）
 * <p>学生端查看单条投递详情的数据结构，含当前快照与状态历史</p>
 *
 * @author atmoto-recruit
 */
@Data
public class ApplicationDetailVo {

    /** 投递记录ID */
    private Long applicationId;

    /** 学生ID */
    private Long studentId;

    /** 岗位ID */
    private Long jobId;

    /** 岗位名称 */
    private String jobTitle;

    /** 所属部门（公司/部门名） */
    private String company;

    /** 投递状态码（英文） */
    private String status;

    /** 投递状态标签（中文） */
    private String statusLabel;

    /** 渠道来源 */
    private String source;

    /** 投递时间 */
    private LocalDateTime applyTime;

    /** 快照版本号 */
    private Integer versionNo;

    /** 快照生成时间 */
    private LocalDateTime snapshotTime;

    /** 快照中的姓名 */
    private String snapshotName;

    /** 快照中的学校 */
    private String snapshotSchool;

    /** 快照中的专业 */
    private String snapshotMajor;

    /** 快照中的学历 */
    private String snapshotDegree;

    /** 简历快照数据（JSON字符串，完整简历信息） */
    private String snapshotProfile;

    /** 教育经历快照数据（JSON字符串） */
    private String snapshotEducations;

    /** 简历附件快照数据（JSON字符串） */
    private String snapshotResumeFile;

    /** 状态流转历史（按时间升序） */
    private List<AppStatusHistory> statusHistory;
}
