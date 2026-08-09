package com.atmoto.recruit.biz.portal.service.impl;

import com.atmoto.recruit.biz.common.domain.JobCategory;
import com.atmoto.recruit.biz.common.domain.JobPosition;
import com.atmoto.recruit.biz.common.enums.JobStatus;
import com.atmoto.recruit.biz.common.mapper.JobCategoryMapper;
import com.atmoto.recruit.biz.common.mapper.JobPositionMapper;
import com.atmoto.recruit.biz.portal.service.PortalJobService;
import com.atmoto.recruit.biz.portal.vo.PortalJobVo;
import com.atmoto.recruit.common.core.page.PageQuery;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.atmoto.recruit.system.domain.SysDept;
import com.atmoto.recruit.system.mapper.SysDeptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 门户端岗位浏览 Service 实现
 * <p>面向学生/求职者的岗位搜索、浏览、筛选功能</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalJobServiceImpl implements PortalJobService {

    private final JobPositionMapper jobPositionMapper;
    private final SysDeptMapper sysDeptMapper;
    private final JobCategoryMapper jobCategoryMapper;

    @Override
    public IPage<JobPosition> selectPublishedJobList(JobPosition jobPosition, PageQuery pageQuery) {
        // 构建分页对象
        Page<JobPosition> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());

        // 使用 QueryWrapper 以支持自定义 SQL（FULLTEXT 搜索）
        QueryWrapper<JobPosition> wrapper = new QueryWrapper<>();

        // 关键词全文搜索：有关键词时使用 MATCH...AGAINST，无关键词时跳过
        String keyword = jobPosition.getTitle();
        if (keyword != null && !keyword.trim().isEmpty()) {
            // ★ 使用 MySQL FULLTEXT 索引进行全文搜索
            wrapper.apply("MATCH(title, description) AGAINST({0} IN NATURAL LANGUAGE MODE)", keyword.trim());
        }

        // 只返回已发布且未过截止日期的岗位
        wrapper.eq("status", JobStatus.PUBLISHED.getCode());
        wrapper.apply("deadline > NOW()");

        // 按部门筛选
        if (jobPosition.getDeptId() != null) {
            wrapper.eq("department_id", jobPosition.getDeptId());
        }
        // 按岗位类别筛选
        if (jobPosition.getCategoryId() != null) {
            wrapper.eq("category_id", jobPosition.getCategoryId());
        }
        // 按工作地点筛选
        if (jobPosition.getLocation() != null && !jobPosition.getLocation().isEmpty()) {
            wrapper.eq("location", jobPosition.getLocation());
        }
        // 按学历要求筛选
        if (jobPosition.getDegreeRequirement() != null && !jobPosition.getDegreeRequirement().isEmpty()) {
            wrapper.eq("degree_requirement", jobPosition.getDegreeRequirement());
        }

        // 按创建时间降序排列
        wrapper.orderByDesc("create_time");

        return jobPositionMapper.selectPage(page, wrapper);
    }

    @Override
    public PortalJobVo selectJobDetail(Long jobId) {
        // 1. 查询岗位
        JobPosition job = jobPositionMapper.selectById(jobId);
        if (job == null) {
            throw new BizException(ErrorCode.JOB_NOT_FOUND);
        }

        // 2. 增加浏览量（MySQL 原子递增）
        JobPosition updateView = new JobPosition();
        updateView.setJobId(jobId);
        // 使用 LambdaUpdateWrapper 做 view_count + 1
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<JobPosition> updateWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        updateWrapper.setSql("view_count = view_count + 1")
                .eq(JobPosition::getJobId, jobId);
        jobPositionMapper.update(null, updateWrapper);
        // 内存中也 +1，保证返回给前端的 viewCount 是最新的
        job.setViewCount(job.getViewCount() != null ? job.getViewCount() + 1 : 1);

        // 3. 组装 VO
        PortalJobVo vo = new PortalJobVo();
        BeanUtils.copyProperties(job, vo);

        // 4. 查询部门名称
        if (job.getDeptId() != null) {
            SysDept dept = sysDeptMapper.selectById(job.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getDeptName());
            }
        }

        // 5. 查询岗位类别名称
        if (job.getCategoryId() != null) {
            JobCategory category = jobCategoryMapper.selectById(job.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }
        }

        return vo;
    }

    @Override
    public Map<String, Object> getFilterOptions() {
        Map<String, Object> options = new LinkedHashMap<>();

        // 1. 部门列表（启用的部门）
        List<SysDept> deptList = sysDeptMapper.selectList(
                new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getStatus, "0")
                        .orderByAsc(SysDept::getOrderNum)
        );
        // 前端只需 id 和 name，做精简
        List<Map<String, Object>> deptSimple = deptList.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("deptId", d.getDeptId());
            m.put("deptName", d.getDeptName());
            return m;
        }).collect(Collectors.toList());
        options.put("departments", deptSimple);

        // 2. 岗位类别树（启用的类别，平铺由前端组装）
        List<JobCategory> categoryList = jobCategoryMapper.selectList(
                new LambdaQueryWrapper<JobCategory>()
                        .eq(JobCategory::getStatus, "0")
                        .orderByAsc(JobCategory::getParentId, JobCategory::getOrderNum)
        );
        options.put("categories", categoryList);

        // 3. 工作地点列表（从已发布岗位中提取 DISTINCT）
        QueryWrapper<JobPosition> locWrapper = new QueryWrapper<>();
        locWrapper.select("DISTINCT location")
                .eq("status", JobStatus.PUBLISHED.getCode())
                .apply("deadline > NOW()")
                .isNotNull("location")
                .orderByAsc("location");
        List<Object> locations = jobPositionMapper.selectObjs(locWrapper);
        options.put("locations", locations);

        // 4. 学历要求列表（从已发布岗位中提取 DISTINCT）
        QueryWrapper<JobPosition> degreeWrapper = new QueryWrapper<>();
        degreeWrapper.select("DISTINCT degree_requirement")
                .eq("status", JobStatus.PUBLISHED.getCode())
                .apply("deadline > NOW()")
                .isNotNull("degree_requirement")
                .orderByAsc("degree_requirement");
        List<Object> degrees = jobPositionMapper.selectObjs(degreeWrapper);
        options.put("degreeRequirements", degrees);

        return options;
    }
}
