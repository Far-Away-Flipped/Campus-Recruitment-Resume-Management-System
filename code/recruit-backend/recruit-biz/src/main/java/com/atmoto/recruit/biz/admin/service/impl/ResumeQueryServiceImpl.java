package com.atmoto.recruit.biz.admin.service.impl;

import com.atmoto.recruit.biz.admin.dto.ResumeQueryDTO;
import com.atmoto.recruit.biz.admin.service.ResumeQueryService;
import com.atmoto.recruit.biz.admin.vo.*;
import com.atmoto.recruit.biz.common.domain.*;
import com.atmoto.recruit.biz.common.enums.ApplicationStatus;
import com.atmoto.recruit.biz.common.mapper.*;
import com.atmoto.recruit.common.core.domain.TableDataInfo;
import com.atmoto.recruit.common.core.page.PageQuery;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 简历查询服务实现
 * <p>HR端多维筛选+分页查询简历列表，含数据范围约束</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeQueryServiceImpl implements ResumeQueryService {

    private final ApplicationMapper applicationMapper;
    private final JobPositionMapper jobPositionMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final StudentEducationMapper studentEducationMapper;
    private final AppSnapshotMapper appSnapshotMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final AppStatusHistoryMapper appStatusHistoryMapper;
    private final AppHrNoteMapper appHrNoteMapper;
    private final ObjectMapper objectMapper;

    @Value("${file.upload-root:E:/atmoto-recruit/data}")
    private String uploadRoot;

    @Override
    public TableDataInfo selectResumeList(ResumeQueryDTO queryDTO, PageQuery pageQuery,
                                          Long operatorUserId, boolean hasAllDataScope) {
        // 构建分页对象
        Page<Application> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());

        // 数据范围约束：非全部权限时限制为当前HR负责的岗位
        Long ownerUserId = hasAllDataScope ? null : operatorUserId;

        // 执行分页查询（含LEFT JOIN job_position的数据范围校验）
        IPage<Application> resultPage = applicationMapper.selectResumePage(
                page,
                queryDTO.getJobId(),
                queryDTO.getStatus(),
                queryDTO.getKeyword(),
                queryDTO.getSchool(),
                queryDTO.getMajor(),
                queryDTO.getDegree(),
                queryDTO.getApplyTimeStart(),
                queryDTO.getApplyTimeEnd(),
                ownerUserId);

        // 批量查询岗位名称，避免 N+1 查询，填充 jobTitle 给前端
        Map<Long, String> jobTitleMap = buildJobTitleMap(resultPage.getRecords());

        // 将 Application 转换为 ResumeListVO
        List<ResumeListVO> voList = resultPage.getRecords().stream()
                .map(app -> toListVO(app, jobTitleMap))
                .collect(Collectors.toList());

        return TableDataInfo.of(resultPage.getTotal(), voList);
    }

    @Override
    public ResumeDetailVO selectResumeDetail(Long applicationId, Long operatorUserId, boolean hasAllDataScope) {
        // 数据范围约束
        Long ownerUserId = hasAllDataScope ? null : operatorUserId;

        // 查询投递记录（含数据范围校验）
        Application app = applicationMapper.selectByIdWithScope(applicationId, ownerUserId);
        if (app == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "投递记录不存在或无权限查看");
        }

        // 构建详情VO
        ResumeDetailVO vo = new ResumeDetailVO();
        vo.setApplicationId(app.getApplicationId());
        vo.setStudentId(app.getStudentId());
        vo.setJobId(app.getJobId());
        vo.setStatus(app.getStatus());
        vo.setApplyTime(app.getApplyTime());
        vo.setSource(app.getSource());

        // 状态中文名
        try {
            vo.setStatusLabel(ApplicationStatus.fromCode(app.getStatus()).getLabel());
        } catch (IllegalArgumentException e) {
            vo.setStatusLabel(app.getStatus());
        }

        // 岗位名称
        JobPosition job = jobPositionMapper.selectById(app.getJobId());
        if (job != null) {
            vo.setJobTitle(job.getTitle());
        }

        // 学生基本资料（姓名/学校/专业/学历从快照冗余列取，保证与列表页一致）
        if (app.getSnapshotName() != null) {
            vo.setStudentName(app.getSnapshotName());
        } else {
            StudentProfile profile = studentProfileMapper.selectOne(
                    new LambdaQueryWrapper<StudentProfile>()
                            .eq(StudentProfile::getStudentId, app.getStudentId()));
            if (profile != null) {
                vo.setStudentName(profile.getName());
                vo.setStudentPhone(profile.getPhone());
                vo.setStudentEmail(profile.getEmail());
                vo.setGender(profile.getGender());
                vo.setAvatarUrl(profile.getAvatarUrl());
                if (profile.getBirthDate() != null) {
                    vo.setBirthDate(profile.getBirthDate().toString());
                }
                vo.setCurrentCity(profile.getCurrentCity());
            }
        }

        // 补充电话/邮箱等无快照列的字段（始终从 stu_profile 读取）
        StudentProfile profile = studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>()
                        .eq(StudentProfile::getStudentId, app.getStudentId()));
        if (profile != null) {
            vo.setStudentPhone(profile.getPhone());
            vo.setStudentEmail(profile.getEmail());
            vo.setGender(profile.getGender());
            vo.setAvatarUrl(profile.getAvatarUrl());
            if (profile.getBirthDate() != null) {
                vo.setBirthDate(profile.getBirthDate().toString());
            }
            vo.setCurrentCity(profile.getCurrentCity());
        }

        // 教育经历：优先取快照（app_snapshot），无快照时取实时数据
        if (app.getCurrentSnapshotId() != null) {
            AppSnapshot snapshot = appSnapshotMapper.selectById(app.getCurrentSnapshotId());
            if (snapshot != null && snapshot.getSnapshotEducations() != null) {
                try {
                    List<Map<String,Object>> snapEdus = objectMapper.readValue(
                        snapshot.getSnapshotEducations(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String,Object>>>() {});
                    vo.setEducations(snapEdus.stream().map(m -> {
                        EducationVO evo = new EducationVO();
                        evo.setSchoolName((String) m.get("schoolName"));
                        evo.setMajor((String) m.get("major"));
                        evo.setDegree((String) m.get("degree"));
                        evo.setStartDate(m.get("startDate") != null ? java.time.LocalDate.parse(m.get("startDate").toString()) : null);
                        evo.setEndDate(m.get("endDate") != null ? java.time.LocalDate.parse(m.get("endDate").toString()) : null);
                        evo.setGpa((String) m.get("gpa")); // 实体属性 gpa(@TableField gpa_rank)，Jackson key 为 gpa
                        return evo;
                    }).collect(Collectors.toList()));
                } catch (Exception e) {
                    log.warn("快照教育经历解析失败,回退到实时数据: applicationId={}", applicationId, e);
                    vo.setEducations(null); // 触达下方实时数据回退
                }
            }
        }
        // 回退：无快照或快照解析失败时取实时教育经历
        if (vo.getEducations() == null) {
            List<StudentEducation> educations = studentEducationMapper.selectList(
                    new LambdaQueryWrapper<StudentEducation>()
                            .eq(StudentEducation::getStudentId, app.getStudentId())
                            .orderByAsc(StudentEducation::getSortOrder));
            if (educations != null) {
                vo.setEducations(educations.stream().map(e -> {
                    EducationVO evo = new EducationVO();
                    evo.setId(e.getId());
                    evo.setSchoolName(e.getSchoolName());
                    evo.setMajor(e.getMajor());
                    evo.setDegree(e.getDegree());
                    evo.setStartDate(e.getStartDate());
                    evo.setEndDate(e.getEndDate());
                    evo.setGpa(e.getGpa());
                    return evo;
                }).collect(Collectors.toList()));
            }
        }

        // 当前快照（最新版本）
        if (app.getCurrentSnapshotId() != null) {
            AppSnapshot snapshot = appSnapshotMapper.selectById(app.getCurrentSnapshotId());
            if (snapshot != null) {
                SnapshotVO svo = new SnapshotVO();
                svo.setSnapshotId(snapshot.getSnapshotId());
                svo.setVersionNo(snapshot.getVersionNo());
                svo.setSnapshotTime(snapshot.getSnapshotTime());
                svo.setSnapshotProfile(snapshot.getSnapshotProfile());
                svo.setSnapshotEducations(snapshot.getSnapshotEducations());
                svo.setSnapshotResumeFile(snapshot.getSnapshotResumeFile());
                vo.setCurrentSnapshot(svo);

                // 实习/项目经历：从快照读（新投递已收录；改造前旧快照三列为 NULL → 留空，不回退实时数据）
                if (snapshot.getSnapshotInternships() != null) {
                    try {
                        List<InternshipBriefVO> list = objectMapper.readValue(
                                snapshot.getSnapshotInternships(),
                                new com.fasterxml.jackson.core.type.TypeReference<List<InternshipBriefVO>>() {});
                        list.forEach(x -> x.setRecordTypeLabel("I".equals(x.getRecordType()) ? "实习经历" : "项目经历"));
                        vo.setInternships(list);
                    } catch (Exception e) {
                        log.warn("快照实习/项目经历解析失败,置空: applicationId={}", applicationId, e);
                    }
                }
                // 技能/证书/语言能力
                if (snapshot.getSnapshotCertificates() != null) {
                    try {
                        List<CertificateBriefVO> list = objectMapper.readValue(
                                snapshot.getSnapshotCertificates(),
                                new com.fasterxml.jackson.core.type.TypeReference<List<CertificateBriefVO>>() {});
                        list.forEach(x -> {
                            switch (x.getCertType()) {
                                case "SKILL" -> x.setCertTypeLabel("技能");
                                case "CERT" -> x.setCertTypeLabel("证书");
                                case "LANGUAGE" -> x.setCertTypeLabel("语言能力");
                                default -> x.setCertTypeLabel(x.getCertType());
                            }
                        });
                        vo.setCertificates(list);
                    } catch (Exception e) {
                        log.warn("快照技能/证书经历解析失败,置空: applicationId={}", applicationId, e);
                    }
                }
                // 社团/校园经历
                if (snapshot.getSnapshotActivities() != null) {
                    try {
                        List<ActivityBriefVO> list = objectMapper.readValue(
                                snapshot.getSnapshotActivities(),
                                new com.fasterxml.jackson.core.type.TypeReference<List<ActivityBriefVO>>() {});
                        vo.setActivities(list);
                    } catch (Exception e) {
                        log.warn("快照社团经历解析失败,置空: applicationId={}", applicationId, e);
                    }
                }
            }
        }
        // 三类经历兜底为空列表（保证前端拿到数组，不因空值渲染异常）
        if (vo.getInternships() == null) vo.setInternships(Collections.emptyList());
        if (vo.getCertificates() == null) vo.setCertificates(Collections.emptyList());
        if (vo.getActivities() == null) vo.setActivities(Collections.emptyList());

        // 附件列表（仅返回磁盘上实际存在的文件）
        List<ResumeFile> files = resumeFileMapper.selectList(
                new LambdaQueryWrapper<ResumeFile>()
                        .eq(ResumeFile::getStudentId, app.getStudentId()));
        if (files != null) {
            String root = uploadRoot != null ? uploadRoot : "E:/atmoto-recruit/data";
            vo.setAttachments(files.stream()
                .filter(f -> {
                    String fp = f.getFilePath();
                    if (fp == null || fp.isEmpty()) return false;
                    java.io.File diskF = new java.io.File(fp);
                    if (!diskF.isAbsolute()) diskF = new java.io.File(root, fp);
                    return diskF.exists();
                })
                .map(f -> {
                AttachmentVO avo = new AttachmentVO();
                avo.setId(f.getId());
                avo.setOriginalName(f.getOriginalName());
                avo.setFileExt(f.getFileExt());
                avo.setFileSize(f.getFileSize());
                avo.setPreviewStatus(f.getPreviewStatus());
                return avo;
            }).collect(Collectors.toList()));
        }

        // 状态流转历史
        List<AppStatusHistory> histories = appStatusHistoryMapper.selectList(
                new LambdaQueryWrapper<AppStatusHistory>()
                        .eq(AppStatusHistory::getApplicationId, applicationId)
                        .orderByAsc(AppStatusHistory::getOperateTime));
        if (histories != null && !histories.isEmpty()) {
            vo.setStatusHistory(histories.stream().map(h -> {
                StatusHistoryVO svo = new StatusHistoryVO();
                svo.setFromStatus(h.getFromStatus());
                svo.setToStatus(h.getToStatus());
                svo.setOperatorType(h.getOperatorType());
                svo.setOperatorId(h.getOperatorId());
                svo.setRemark(h.getRemark());
                svo.setOperateTime(h.getOperateTime());
                return svo;
            }).collect(Collectors.toList()));
        }

        // HR 内部备注
        List<AppHrNote> hrNotes = appHrNoteMapper.selectList(
                new LambdaQueryWrapper<AppHrNote>()
                        .eq(AppHrNote::getApplicationId, applicationId)
                        .orderByAsc(AppHrNote::getCreateTime));
        if (hrNotes != null && !hrNotes.isEmpty()) {
            vo.setRemarks(hrNotes.stream().map(n -> {
                HrNoteVO rvo = new HrNoteVO();
                rvo.setId(n.getId());
                rvo.setApplicationId(n.getApplicationId());
                rvo.setContent(n.getContent());
                rvo.setOperatorId(n.getOperatorId());
                rvo.setOperatorName(n.getOperatorName());
                rvo.setCreateTime(n.getCreateTime());
                return rvo;
            }).collect(Collectors.toList()));
        }

        return vo;
    }

    /**
     * Application 转 ResumeListVO
     * @param app 投递记录
     * @param jobTitleMap 岗位ID→岗位名称映射（由 buildJobTitleMap 批量查询生成）
     */
    private ResumeListVO toListVO(Application app, Map<Long, String> jobTitleMap) {
        ResumeListVO vo = new ResumeListVO();
        vo.setApplicationId(app.getApplicationId());
        vo.setStudentId(app.getStudentId());
        vo.setJobId(app.getJobId());
        vo.setJobTitle(jobTitleMap.getOrDefault(app.getJobId(), ""));
        vo.setStatus(app.getStatus());
        vo.setStudentName(app.getSnapshotName());
        vo.setSchool(app.getSnapshotSchool());
        vo.setMajor(app.getSnapshotMajor());
        vo.setDegree(app.getSnapshotDegree());
        vo.setApplyTime(app.getApplyTime());

        // 状态中文名
        try {
            vo.setStatusLabel(ApplicationStatus.fromCode(app.getStatus()).getLabel());
        } catch (IllegalArgumentException e) {
            vo.setStatusLabel(app.getStatus());
        }

        return vo;
    }

    /**
     * 批量查询投递记录关联的岗位标题
     * <p>收集所有不重复的 jobId，一次性批量查询，构建 jobId→title 映射</p>
     */
    private Map<Long, String> buildJobTitleMap(List<Application> applications) {
        Set<Long> jobIds = applications.stream()
                .map(Application::getJobId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (jobIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<JobPosition> jobs = jobPositionMapper.selectBatchIds(jobIds);
        return jobs.stream()
                .collect(Collectors.toMap(JobPosition::getJobId, JobPosition::getTitle, (a, b) -> a));
    }
}
