package com.atmoto.recruit.biz.common.service.impl;

import com.atmoto.recruit.biz.common.domain.*;
import com.atmoto.recruit.biz.common.enums.ApplicationStatus;
import com.atmoto.recruit.biz.common.enums.JobStatus;
import com.atmoto.recruit.biz.common.enums.SourceChannel;
import com.atmoto.recruit.biz.common.mapper.*;
import com.atmoto.recruit.biz.common.service.ApplicationService;
import com.atmoto.recruit.biz.notify.NotifyService;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 投递核心 Service 实现（M4 投递流程）
 * <p>
 * 包含投递的完整校验链、简历快照生成、状态历史记录、岗位计数更新与异步通知。
 * 所有操作在同一个事务中完成，保证数据一致性。
 * </p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final JobPositionMapper jobPositionMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final StudentEducationMapper studentEducationMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final ApplicationMapper applicationMapper;
    private final AppSnapshotMapper appSnapshotMapper;
    private final AppStatusHistoryMapper appStatusHistoryMapper;
    private final StudentInternshipMapper studentInternshipMapper;
    private final StudentCertificateMapper studentCertificateMapper;
    private final StudentActivityMapper studentActivityMapper;
    private final NotifyService notifyService;
    private final ObjectMapper objectMapper;

    /**
     * 学生投递岗位（核心事务）
     * <p>
     * 完整校验链：岗位校验 → 资料完整性 → 唯一约束（DB层面）→ 乐观锁
     * 事务内：快照生成 → 投递记录 → 状态历史 → 岗位计数 → 异步通知
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Application submitApplication(Long studentId, Long jobId, String source, Long fileId) {
        // ═══════════════════════════════════════════
        // 校验 1：岗位存在且已发布且未截止
        // ═══════════════════════════════════════════
        JobPosition job = jobPositionMapper.selectById(jobId);
        if (job == null) {
            throw new BizException(ErrorCode.JOB_NOT_FOUND);
        }
        // EXPIRED 为实时派生态（V6 后不持久化），允许通过；真过期由下方 deadline 检查兜底
        boolean publishable = JobStatus.PUBLISHED.getCode().equals(job.getStatus())
                || JobStatus.EXPIRED.getCode().equals(job.getStatus());
        if (!publishable) {
            throw new BizException(ErrorCode.JOB_NOT_PUBLISHED);
        }
        if (job.getDeadline() != null && job.getDeadline().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.JOB_DEADLINE_EXPIRED);
        }

        // ═══════════════════════════════════════════
        // 校验 2：学生资料完整性
        // ═══════════════════════════════════════════
        // 2a. 基本资料必须存在
        StudentProfile profile = studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>()
                        .eq(StudentProfile::getStudentId, studentId));
        if (profile == null) {
            throw new BizException(ErrorCode.STUDENT_PROFILE_INCOMPLETE);
        }

        // 2b. 教育经历至少一条
        List<StudentEducation> educations = studentEducationMapper.selectList(
                new LambdaQueryWrapper<StudentEducation>()
                        .eq(StudentEducation::getStudentId, studentId)
                        .orderByAsc(StudentEducation::getSortOrder));
        if (educations == null || educations.isEmpty()) {
            throw new BizException(ErrorCode.STUDENT_EDUCATION_MISSING);
        }

        // 2c. 简历附件至少一条
        Long resumeCount = resumeFileMapper.selectCount(
                new LambdaQueryWrapper<ResumeFile>()
                        .eq(ResumeFile::getStudentId, studentId));
        if (resumeCount == null || resumeCount == 0) {
            throw new BizException(ErrorCode.STUDENT_RESUME_MISSING);
        }

        // ═══════════════════════════════════════════
        // 校验 3：重复投递由 DB uk_student_job 唯一约束保障
        //         （GlobalExceptionHandler 捕获 DuplicateKeyException 返回 APPLICATION_DUPLICATE）
        // ═══════════════════════════════════════════

        // ═══════════════════════════════════════════
        // 校验 4：乐观锁 —— Application.version 字段
        //         首次插入 version=1，后续更新时校验
        // ═══════════════════════════════════════════

        // ═══════════════════════════════════════════
        // 5. 根据 fileId 选取指定简历附件, 未指定则默认取第一条
        // ═══════════════════════════════════════════
        List<ResumeFile> resumeFiles = resumeFileMapper.selectList(
                new LambdaQueryWrapper<ResumeFile>()
                        .eq(ResumeFile::getStudentId, studentId));

        ResumeFile selectedFile = null;
        if (fileId != null) {
            selectedFile = resumeFileMapper.selectOne(
                    new LambdaQueryWrapper<ResumeFile>()
                            .eq(ResumeFile::getId, fileId)
                            .eq(ResumeFile::getStudentId, studentId)); // 归属校验
            if (selectedFile == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "指定的简历附件不存在");
            }
        } else if (resumeFiles != null && !resumeFiles.isEmpty()) {
            selectedFile = resumeFiles.get(0);
        }
        String resumeFileJson = selectedFile != null ? toJson(selectedFile) : null;

        // ═══════════════════════════════════════════
        // 6. 生成简历快照 JSON
        // ═══════════════════════════════════════════
        String profileJson = toJson(profile);
        String educationsJson = toJson(educations);

        // 6.1 查询并序列化实习/项目、技能/证书、社团经历（可选信息，无则空数组；收录进快照供后台/学生端查看投递当时的完整简历）
        List<StudentInternship> internships = studentInternshipMapper.selectList(
                new LambdaQueryWrapper<StudentInternship>()
                        .eq(StudentInternship::getStudentId, studentId)
                        .orderByAsc(StudentInternship::getSortOrder));
        List<StudentCertificate> certificates = studentCertificateMapper.selectList(
                new LambdaQueryWrapper<StudentCertificate>()
                        .eq(StudentCertificate::getStudentId, studentId)
                        .orderByAsc(StudentCertificate::getSortOrder));
        List<StudentActivity> activities = studentActivityMapper.selectList(
                new LambdaQueryWrapper<StudentActivity>()
                        .eq(StudentActivity::getStudentId, studentId)
                        .orderByAsc(StudentActivity::getSortOrder));
        String internshipsJson = toJson(internships != null ? internships : Collections.emptyList());
        String certificatesJson = toJson(certificates != null ? certificates : Collections.emptyList());
        String activitiesJson = toJson(activities != null ? activities : Collections.emptyList());

        // 取第一条教育经历（按 sortOrder 升序，最低的排最前）作为冗余筛选字段
        StudentEducation highestEdu = educations.get(0);

        // ═══════════════════════════════════════════
        // 7. INSERT app_snapshot（快照不可变，仅追加）
        // ═══════════════════════════════════════════
        AppSnapshot snapshot = new AppSnapshot();
        snapshot.setStudentId(studentId);
        snapshot.setVersionNo(1);
        snapshot.setSnapshotTime(LocalDateTime.now());
        snapshot.setSnapshotProfile(profileJson);
        snapshot.setSnapshotEducations(educationsJson);
        snapshot.setSnapshotInternships(internshipsJson);
        snapshot.setSnapshotCertificates(certificatesJson);
        snapshot.setSnapshotActivities(activitiesJson);
        snapshot.setSnapshotResumeFile(resumeFileJson);
        appSnapshotMapper.insert(snapshot);

        // ═══════════════════════════════════════════
        // 8. INSERT app_application（status=PENDING_SCREEN）
        // ═══════════════════════════════════════════
        Application application = new Application();
        application.setStudentId(studentId);
        application.setJobId(jobId);
        application.setStatus(ApplicationStatus.PENDING_SCREEN.getCode());
        application.setVersion(1);
        application.setCurrentSnapshotId(snapshot.getSnapshotId());
        application.setSource(source != null && !source.isEmpty()
                ? source : SourceChannel.OFFICIAL_SITE.getCode());
        application.setApplyTime(LocalDateTime.now());
        // 筛选冗余字段
        application.setSnapshotName(profile.getName());
        application.setSnapshotSchool(highestEdu.getSchoolName());
        application.setSnapshotMajor(highestEdu.getMajor());
        application.setSnapshotDegree(highestEdu.getDegree());
        applicationMapper.insert(application);

        // 回填快照的 applicationId
        snapshot.setApplicationId(application.getApplicationId());
        appSnapshotMapper.updateById(snapshot);

        // ═══════════════════════════════════════════
        // 9. INSERT app_status_history（初态：from_status=null → to_status=PENDING_SCREEN）
        // ═══════════════════════════════════════════
        AppStatusHistory history = new AppStatusHistory();
        history.setApplicationId(application.getApplicationId());
        history.setFromStatus(null);
        history.setToStatus(ApplicationStatus.PENDING_SCREEN.getCode());
        history.setOperatorType("STUDENT");
        history.setOperatorId(studentId);
        history.setOperateTime(LocalDateTime.now());
        history.setRemark("学生投递");
        appStatusHistoryMapper.insert(history);

        // ═══════════════════════════════════════════
        // 10. UPDATE job_position SET application_count + 1（MySQL 原子自增）
        // ═══════════════════════════════════════════
        LambdaUpdateWrapper<JobPosition> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.setSql("apply_count = apply_count + 1")
                .eq(JobPosition::getJobId, jobId);
        jobPositionMapper.update(null, updateWrapper);

        log.info("投递成功：studentId={}, jobId={}, applicationId={}, source={}",
                studentId, jobId, application.getApplicationId(), application.getSource());

        // ═══════════════════════════════════════════
        // 11. 异步发送站内信通知（学生 + HR）
        //     NotifyService 方法标记 @Async，不阻塞主事务
        // ═══════════════════════════════════════════
        String studentTitle = "投递成功";
        String studentContent = "您已成功投递岗位【" + job.getTitle() + "】，请耐心等待HR筛选。";
        notifyService.sendInAppMessage(studentId, studentTitle, studentContent);

        String hrTitle = "新投递提醒";
        String hrContent = "有学生投递了您负责的岗位【" + job.getTitle() + "】，请及时处理。";
        notifyService.sendHrNotification(jobId, hrTitle, hrContent);

        return application;
    }

    /**
     * 对象转 JSON 字符串，序列化失败时包装为 BizException
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("快照JSON序列化失败：{}", obj.getClass().getSimpleName(), e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "快照数据序列化失败");
        }
    }
}
