package com.atmoto.recruit.biz.admin.service.impl;

import com.atmoto.recruit.biz.admin.service.JobPositionService;
import com.atmoto.recruit.biz.common.domain.JobPosition;
import com.atmoto.recruit.biz.common.enums.JobStatus;
import com.atmoto.recruit.biz.common.mapper.JobPositionMapper;
import com.atmoto.recruit.biz.common.util.JobStatusResolver;
import com.atmoto.recruit.common.core.page.PageQuery;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.atmoto.recruit.system.mapper.SysDeptMapper;
import com.atmoto.recruit.system.util.DeptTreeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 岗位管理 Service 实现
 * <p>管理后台岗位的增删改查、发布、下架操作</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobPositionServiceImpl implements JobPositionService {

    private final JobPositionMapper jobPositionMapper;
    private final SysDeptMapper deptMapper;

    /** 前端排序字段名 → MyBatis-Plus Lambda 列引用映射（deptName 非持久字段，使用 deptId 排序） */
    private static final Map<String, SFunction<JobPosition, ?>> SORT_COLUMN_MAP = new HashMap<>();
    static {
        SORT_COLUMN_MAP.put("title", JobPosition::getTitle);
        SORT_COLUMN_MAP.put("deptName", JobPosition::getDeptId);
        SORT_COLUMN_MAP.put("location", JobPosition::getLocation);
        SORT_COLUMN_MAP.put("status", JobPosition::getStatus);
        SORT_COLUMN_MAP.put("deadline", JobPosition::getDeadline);
        SORT_COLUMN_MAP.put("applicationCount", JobPosition::getApplicationCount);
    }

    @Override
    public IPage<JobPosition> selectJobList(JobPosition jobPosition, PageQuery pageQuery) {
        // 构建分页对象
        Page<JobPosition> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());

        // 构建查询条件
        LambdaQueryWrapper<JobPosition> wrapper = new LambdaQueryWrapper<>();

        // 按标题模糊搜索
        if (jobPosition.getTitle() != null && !jobPosition.getTitle().isEmpty()) {
            wrapper.like(JobPosition::getTitle, jobPosition.getTitle());
        }
        // 按状态筛选
        if (jobPosition.getStatus() != null && !jobPosition.getStatus().isEmpty()) {
            wrapper.eq(JobPosition::getStatus, jobPosition.getStatus());
        }
        // 按部门筛选：选择父部门时，需包含其全部子部门的岗位
        if (jobPosition.getDeptId() != null) {
            List<Long> deptIds = DeptTreeUtil.collectDeptAndDescendants(jobPosition.getDeptId(), deptMapper, false);
            if (!deptIds.isEmpty()) {
                wrapper.in(JobPosition::getDeptId, deptIds);
            }
        }
        // 按岗位类别筛选
        if (jobPosition.getCategoryId() != null) {
            wrapper.eq(JobPosition::getCategoryId, jobPosition.getCategoryId());
        }
        // 按工作地点筛选（location 存 JSON 数组文本，用 LIKE 子串匹配）
        if (jobPosition.getLocation() != null && !jobPosition.getLocation().isBlank()) {
            wrapper.like(JobPosition::getLocation, jobPosition.getLocation());
        }
        // 按学历要求筛选
        if (jobPosition.getDegreeRequirement() != null && !jobPosition.getDegreeRequirement().isBlank()) {
            wrapper.eq(JobPosition::getDegreeRequirement, jobPosition.getDegreeRequirement());
        }

        // 动态排序：前端传入 sortField/sortOrder，通过 PageQuery 传递
        String sortField = pageQuery.getOrderByColumn();
        if (sortField != null && !sortField.isBlank()) {
            SFunction<JobPosition, ?> sortColumn = SORT_COLUMN_MAP.get(sortField);
            if (sortColumn != null) {
                boolean isAsc = "asc".equalsIgnoreCase(pageQuery.getIsAsc());
                wrapper.orderBy(true, isAsc, sortColumn);
            }
        }
        // 默认按创建时间降序（无动态排序时作为主排序，有时作为次级排序）
        wrapper.orderByDesc(JobPosition::getCreateTime);

        jobPositionMapper.selectPage(page, wrapper);
        // 输出层实时计算过期状态（PUBLISHED 且 deadline 已过 → EXPIRED，仅展示，不持久化）
        if (page.getRecords() != null) {
            LocalDateTime now = LocalDateTime.now();
            page.getRecords().forEach(j ->
                    j.setStatus(JobStatusResolver.resolveDisplayStatus(j.getStatus(), j.getDeadline(), now)));
        }
        return page;
    }

    @Override
    public JobPosition selectJobById(Long jobId) {
        JobPosition job = jobPositionMapper.selectById(jobId);
        if (job == null) {
            throw new BizException(ErrorCode.JOB_NOT_FOUND);
        }
        // 输出层实时计算过期状态
        job.setStatus(JobStatusResolver.resolveDisplayStatus(job.getStatus(), job.getDeadline(), LocalDateTime.now()));
        return job;
    }

    @Override
    public int insertJob(JobPosition jobPosition) {
        // 创建时仅允许 DRAFT 状态；如果传了 PUBLISHED/CLOSED，重置为 DRAFT 并打印警告
        if (jobPosition.getStatus() != null && !JobStatus.DRAFT.getCode().equals(jobPosition.getStatus())) {
            log.warn("创建岗位时传入了非草稿状态 {} 将被忽略，强制设置为 DRAFT", jobPosition.getStatus());
        }
        jobPosition.setStatus(JobStatus.DRAFT.getCode());
        return jobPositionMapper.insert(jobPosition);
    }

    @Override
    public int updateJob(JobPosition jobPosition) {
        return jobPositionMapper.updateById(jobPosition);
    }

    @Override
    public int publishJob(Long jobId) {
        // 先查询岗位当前状态
        JobPosition job = jobPositionMapper.selectById(jobId);
        if (job == null) {
            throw new BizException(ErrorCode.JOB_NOT_FOUND);
        }
        // 仅 DRAFT 或 CLOSED 状态可发布
        String currentStatus = job.getStatus();
        if (!JobStatus.DRAFT.getCode().equals(currentStatus)
                && !JobStatus.CLOSED.getCode().equals(currentStatus)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "仅草稿或已下架状态的岗位可发布，当前状态：" + currentStatus);
        }
        // 更新状态为 PUBLISHED
        JobPosition update = new JobPosition();
        update.setJobId(jobId);
        update.setStatus(JobStatus.PUBLISHED.getCode());
        return jobPositionMapper.updateById(update);
    }

    @Override
    public int offlineJob(Long jobId) {
        // 先查询岗位当前状态
        JobPosition job = jobPositionMapper.selectById(jobId);
        if (job == null) {
            throw new BizException(ErrorCode.JOB_NOT_FOUND);
        }
        // 仅 PUBLISHED 状态可下架
        if (!JobStatus.PUBLISHED.getCode().equals(job.getStatus())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "仅已发布状态的岗位可下架，当前状态：" + job.getStatus());
        }
        // 更新状态为 CLOSED
        JobPosition update = new JobPosition();
        update.setJobId(jobId);
        update.setStatus(JobStatus.CLOSED.getCode());
        return jobPositionMapper.updateById(update);
    }

    @Override
    public int deleteJob(Long jobId) {
        // MyBatis-Plus 逻辑删除（del_flag 置为 2）
        return jobPositionMapper.deleteById(jobId);
    }
}
