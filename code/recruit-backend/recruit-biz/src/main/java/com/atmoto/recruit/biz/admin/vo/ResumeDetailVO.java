package com.atmoto.recruit.biz.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历详情VO
 * <p>含学生基本资料、教育经历、当前快照版本、附件列表</p>
 *
 * @author atmoto-recruit
 */
@Data
public class ResumeDetailVO {

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

    /** 投递时间 */
    private LocalDateTime applyTime;

    /** 渠道来源 */
    private String source;

    // ── 学生基本资料 ──
    private String studentName;
    private String studentPhone;
    private String studentEmail;
    private String gender;
    private String birthDate;
    private String currentCity;
    private String avatarUrl;

    // ── 前端兼容字段（snapshotName 等别名，与前端模板对齐）──
    public String getSnapshotName() { return studentName; }
    public String getSnapshotPhone() { return studentPhone; }
    public String getSnapshotEmail() { return studentEmail; }
    public String getSnapshotGender() { return gender; }
    public String getSnapshotBirth() { return birthDate; }
    public String getSnapshotCity() { return currentCity; }

    // ── 教育经历列表 ──
    private List<EducationVO> educations;

    // ── 实习/项目经历列表（投递快照，旧投递可能为空）──
    private List<InternshipBriefVO> internships;

    // ── 技能/证书/语言能力列表（投递快照，旧投递可能为空）──
    private List<CertificateBriefVO> certificates;

    // ── 社团/校园经历列表（投递快照，旧投递可能为空）──
    private List<ActivityBriefVO> activities;

    // ── 当前快照（最近版本） ──
    private SnapshotVO currentSnapshot;

    // ── 附件列表 ──
    private List<AttachmentVO> attachments;

    // ── HR内部备注列表 ──
    private List<HrNoteVO> remarks;

    // ── 状态流转历史 ──
    private List<StatusHistoryVO> statusHistory;
}
