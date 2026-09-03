package com.atmoto.recruit.biz.portal.service.impl;

import com.atmoto.recruit.biz.common.domain.*;
import com.atmoto.recruit.biz.common.enums.ApplicationStatus;
import com.atmoto.recruit.biz.common.mapper.*;
import com.atmoto.recruit.biz.portal.service.PortalApplicationService;
import com.atmoto.recruit.biz.portal.vo.ApplicationDetailVo;
import com.atmoto.recruit.biz.portal.vo.ApplicationListVo;
import com.atmoto.recruit.common.core.page.PageQuery;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.atmoto.recruit.system.domain.SysDept;
import com.atmoto.recruit.system.mapper.SysDeptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 门户端投递查询 Service 实现（M4 投递流程 - 学生端查询）
 * <p>面向学生/求职者的投递记录查看功能，所有查询均校验数据归属权</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalApplicationServiceImpl implements PortalApplicationService {

    private final ApplicationMapper applicationMapper;
    private final AppSnapshotMapper appSnapshotMapper;
    private final AppStatusHistoryMapper appStatusHistoryMapper;
    private final JobPositionMapper jobPositionMapper;
    private final SysDeptMapper sysDeptMapper;

    /**
     * 查询我的投递列表（分页）
     * <p>连表逻辑：Application → JobPosition（岗位名）→ SysDept（部门名=公司名）。
     * 按投递时间降序排列。不返回已逻辑删除的投递记录。</p>
     */
    @Override
    public IPage<ApplicationListVo> getMyApplications(Long studentId, PageQuery pageQuery) {
        // 1. 分页查询投递记录
        Page<Application> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<Application> wrapper = new LambdaQueryWrapper<Application>()
                .eq(Application::getStudentId, studentId)
                .orderByDesc(Application::getApplyTime);
        IPage<Application> appPage = applicationMapper.selectPage(page, wrapper);

        List<Application> applications = appPage.getRecords();
        if (applications.isEmpty()) {
            // 空结果直接返回
            Page<ApplicationListVo> emptyPage = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
            emptyPage.setTotal(0);
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }

        // 2. 批量查询关联的岗位
        Set<Long> jobIds = applications.stream()
                .map(Application::getJobId)
                .collect(Collectors.toSet());
        List<JobPosition> jobs = jobPositionMapper.selectBatchIds(jobIds);
        Map<Long, JobPosition> jobMap = jobs.stream()
                .collect(Collectors.toMap(JobPosition::getJobId, j -> j, (a, b) -> a));

        // 3. 批量查询关联的部门
        Set<Long> deptIds = jobs.stream()
                .map(JobPosition::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> deptNameMap;
        if (!deptIds.isEmpty()) {
            List<SysDept> depts = sysDeptMapper.selectBatchIds(deptIds);
            deptNameMap = depts.stream()
                    .collect(Collectors.toMap(SysDept::getDeptId, SysDept::getDeptName, (a, b) -> a));
        } else {
            deptNameMap = new HashMap<>();
        }

        // 4. 组装 VO 列表
        List<ApplicationListVo> voList = applications.stream().map(app -> {
            ApplicationListVo vo = new ApplicationListVo();
            vo.setApplicationId(app.getApplicationId());
            vo.setStatus(app.getStatus());
            // 状态中文标签
            try {
                vo.setStatusLabel(ApplicationStatus.fromCode(app.getStatus()).getLabel());
            } catch (IllegalArgumentException e) {
                vo.setStatusLabel(app.getStatus());
            }
            vo.setApplyTime(app.getApplyTime());

            // 岗位信息
            JobPosition job = jobMap.get(app.getJobId());
            if (job != null) {
                vo.setJobTitle(job.getTitle());
                // 部门名称作为公司名
                if (job.getDeptId() != null) {
                    vo.setCompany(deptNameMap.getOrDefault(job.getDeptId(), ""));
                }
            }
            return vo;
        }).collect(Collectors.toList());

        // 5. 构造分页返回
        Page<ApplicationListVo> resultPage = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        resultPage.setTotal(appPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    /**
     * 查询投递详情（含快照与状态流转历史）
     * <p>严格校验归属权：投递记录必须属于当前学生，否则抛出资源不存在异常</p>
     */
    @Override
    public ApplicationDetailVo getApplicationDetail(Long applicationId, Long studentId) {
        // 1. 查询投递记录
        Application application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 2. 归属校验：投递记录必须属于当前学生
        if (!studentId.equals(application.getStudentId())) {
            log.warn("越权访问投递详情：applicationId={}, 请求studentId={}, 实际studentId={}",
                    applicationId, studentId, application.getStudentId());
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 3. 查询当前快照
        AppSnapshot snapshot = null;
        if (application.getCurrentSnapshotId() != null) {
            snapshot = appSnapshotMapper.selectById(application.getCurrentSnapshotId());
        }

        // 4. 查询状态流转历史（按操作时间升序）
        List<AppStatusHistory> statusHistory = appStatusHistoryMapper.selectList(
                new LambdaQueryWrapper<AppStatusHistory>()
                        .eq(AppStatusHistory::getApplicationId, applicationId)
                        .orderByAsc(AppStatusHistory::getOperateTime));

        // 5. 查询岗位信息
        JobPosition job = jobPositionMapper.selectById(application.getJobId());
        String jobTitle = job != null ? job.getTitle() : "";
        String company = "";
        if (job != null && job.getDeptId() != null) {
            SysDept dept = sysDeptMapper.selectById(job.getDeptId());
            if (dept != null) {
                company = dept.getDeptName();
            }
        }

        // 6. 组装 VO
        ApplicationDetailVo vo = new ApplicationDetailVo();
        vo.setApplicationId(application.getApplicationId());
        vo.setStudentId(application.getStudentId());
        vo.setJobId(application.getJobId());
        vo.setJobTitle(jobTitle);
        vo.setCompany(company);
        vo.setStatus(application.getStatus());
        try {
            vo.setStatusLabel(ApplicationStatus.fromCode(application.getStatus()).getLabel());
        } catch (IllegalArgumentException e) {
            vo.setStatusLabel(application.getStatus());
        }
        vo.setSource(application.getSource());
        // 来源渠道展示读中文快照，兼容存量/异常时兜底原值
        vo.setSourceLabel(application.getSourceLabel() != null ? application.getSourceLabel() : application.getSource());
        vo.setApplyTime(application.getApplyTime());

        // 快照信息
        if (snapshot != null) {
            vo.setVersionNo(snapshot.getVersionNo());
            vo.setSnapshotTime(snapshot.getSnapshotTime());
            vo.setSnapshotProfile(snapshot.getSnapshotProfile());
            vo.setSnapshotEducations(snapshot.getSnapshotEducations());
            vo.setSnapshotInternships(snapshot.getSnapshotInternships());
            vo.setSnapshotCertificates(snapshot.getSnapshotCertificates());
            vo.setSnapshotActivities(snapshot.getSnapshotActivities());
            vo.setSnapshotResumeFile(snapshot.getSnapshotResumeFile());
        }

        // 冗余筛选字段（来自 Application 表）
        vo.setSnapshotName(application.getSnapshotName());
        vo.setSnapshotSchool(application.getSnapshotSchool());
        vo.setSnapshotMajor(application.getSnapshotMajor());
        vo.setSnapshotDegree(application.getSnapshotDegree());

        // 状态历史
        vo.setStatusHistory(statusHistory);

        return vo;
    }
}
