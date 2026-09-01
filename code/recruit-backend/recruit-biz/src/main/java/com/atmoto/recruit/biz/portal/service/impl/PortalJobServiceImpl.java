package com.atmoto.recruit.biz.portal.service.impl;

import com.atmoto.recruit.biz.common.domain.Application;
import com.atmoto.recruit.biz.common.domain.JobCategory;
import com.atmoto.recruit.biz.common.domain.JobPosition;
import com.atmoto.recruit.biz.common.enums.JobStatus;
import com.atmoto.recruit.biz.common.mapper.ApplicationMapper;
import com.atmoto.recruit.biz.common.mapper.JobCategoryMapper;
import com.atmoto.recruit.biz.common.mapper.JobPositionMapper;
import com.atmoto.recruit.biz.common.util.JobStatusResolver;
import com.atmoto.recruit.biz.portal.service.PortalJobService;
import com.atmoto.recruit.biz.portal.vo.PortalJobVo;
import com.atmoto.recruit.common.core.page.PageQuery;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.atmoto.recruit.framework.security.context.PortalUserHolder;
import com.atmoto.recruit.system.domain.SysDept;
import com.atmoto.recruit.system.domain.SysDictData;
import com.atmoto.recruit.system.mapper.SysDeptMapper;
import com.atmoto.recruit.system.service.ISysDictDataService;
import com.atmoto.recruit.system.util.DeptTreeUtil;
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
    private final ApplicationMapper applicationMapper;
    private final ISysDictDataService dictDataService;

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

        // 按部门筛选：选择父部门时，需包含其全部子部门的岗位
        if (jobPosition.getDeptId() != null) {
            List<Long> deptIds = DeptTreeUtil.collectDeptAndDescendants(jobPosition.getDeptId(), sysDeptMapper, true);
            if (deptIds.isEmpty()) {
                // 部门不存在或已停用 → 查不到任何岗位
                return page;
            }
            wrapper.in("department_id", deptIds);
        }
        // 按岗位类别筛选：选择父类别时，需包含其全部子类别的岗位
        if (jobPosition.getCategoryId() != null) {
            List<Long> categoryIds = collectCategoryAndDescendants(jobPosition.getCategoryId());
            if (categoryIds.isEmpty()) {
                // 类别不存在或已停用 → 查不到任何岗位
                return page;
            }
            wrapper.in("category_id", categoryIds);
        }
        // 按工作地点筛选（location 存 JSON 数组文本，如 ["BEIJING","SHANGHAI"]，用 LIKE 子串匹配）
        if (jobPosition.getLocation() != null && !jobPosition.getLocation().isEmpty()) {
            wrapper.like("location", jobPosition.getLocation());
        }
        // 按学历要求筛选
        if (jobPosition.getDegreeRequirement() != null && !jobPosition.getDegreeRequirement().isEmpty()) {
            wrapper.eq("degree_requirement", jobPosition.getDegreeRequirement());
        }

        // 按创建时间降序排列
        wrapper.orderByDesc("create_time");

        jobPositionMapper.selectPage(page, wrapper);

        // 批量填充部门名称与岗位类别名称（@TableField(exist=false) 非持久字段，需手动填充）
        if (page.getRecords() != null && !page.getRecords().isEmpty()) {
            Set<Long> deptIds = page.getRecords().stream()
                    .map(JobPosition::getDeptId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!deptIds.isEmpty()) {
                Map<Long, String> deptNameMap = sysDeptMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(SysDept::getDeptId, SysDept::getDeptName, (a, b) -> a));
                page.getRecords().forEach(j -> j.setDeptName(deptNameMap.getOrDefault(j.getDeptId(), "")));
            }
            Set<Long> categoryIds = page.getRecords().stream()
                    .map(JobPosition::getCategoryId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!categoryIds.isEmpty()) {
                Map<Long, String> catNameMap = jobCategoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(JobCategory::getCategoryId, JobCategory::getCategoryName, (a, b) -> a));
                page.getRecords().forEach(j -> j.setCategoryName(catNameMap.getOrDefault(j.getCategoryId(), "")));
            }
        }

        return page;
    }

    /**
     * 收集目标类别自身及其全部后代类别的 ID
     * <p>用于类别筛选时选择父节点能查出该父节点下全部子节点的岗位。
     * job_category 的 ancestors 存祖先链（如 "0,1"），用 CONCAT 包裹做精确子串匹配，
     * 避免 categoryId 与祖先数字串误匹配（如 1 不误命中 "0,11"）。</p>
     */
    private List<Long> collectCategoryAndDescendants(Long categoryId) {
        List<JobCategory> all = jobCategoryMapper.selectList(
                new LambdaQueryWrapper<JobCategory>()
                        .eq(JobCategory::getStatus, "1")
        );
        List<Long> result = new ArrayList<>();
        for (JobCategory cat : all) {
            if (cat.getCategoryId().equals(categoryId)) {
                result.add(cat.getCategoryId());
                continue;
            }
            String ancestors = cat.getAncestors();
            if (ancestors != null && ("," + ancestors + ",").contains("," + categoryId + ",")) {
                result.add(cat.getCategoryId());
            }
        }
        return result;
    }

    @Override
    public PortalJobVo selectJobDetail(Long jobId) {
        // 1. 查询岗位
        JobPosition job = jobPositionMapper.selectById(jobId);
        if (job == null) {
            throw new BizException(ErrorCode.JOB_NOT_FOUND);
        }

        // 2. 校验岗位状态：仅已发布（或存量 EXPIRED 派生态）岗位可被查看；
        //    CLOSED/DRAFT/未知状态一律视为不存在
        boolean viewable = JobStatus.PUBLISHED.getCode().equals(job.getStatus())
                || JobStatus.EXPIRED.getCode().equals(job.getStatus());
        if (!viewable) {
            throw new BizException(ErrorCode.JOB_NOT_FOUND);
        }
        // 3. 实时判断是否已过截止日期（后端服务时间）
        if (job.getDeadline() != null && job.getDeadline().isBefore(java.time.LocalDateTime.now())) {
            throw new BizException(ErrorCode.JOB_DEADLINE_EXPIRED);
        }

        // 3. 增加浏览量（MySQL 原子递增）
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

        // 4. 组装 VO
        PortalJobVo vo = new PortalJobVo();
        BeanUtils.copyProperties(job, vo);
        // 输出层实时归一化展示状态（EXPIRED 为派生态，不持久化）
        vo.setStatus(JobStatusResolver.resolveDisplayStatus(
                job.getStatus(), job.getDeadline(), java.time.LocalDateTime.now()));

        // 5. 查询部门名称
        if (job.getDeptId() != null) {
            SysDept dept = sysDeptMapper.selectById(job.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getDeptName());
            }
        }

        // 6. 查询岗位类别名称
        if (job.getCategoryId() != null) {
            JobCategory category = jobCategoryMapper.selectById(job.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }
        }

        // 7. 查询当前学生是否已投递该岗位
        Long currentStudentId = PortalUserHolder.get();
        if (currentStudentId != null) {
            Long hasApplied = applicationMapper.selectCount(
                    new LambdaQueryWrapper<Application>()
                            .eq(Application::getStudentId, currentStudentId)
                            .eq(Application::getJobId, jobId)
            );
            vo.setHasApplied(hasApplied != null && hasApplied > 0);
        } else {
            vo.setHasApplied(false);
        }

        return vo;
    }

    @Override
    public Map<String, Object> getFilterOptions() {
        Map<String, Object> options = new LinkedHashMap<>();

        // 1. 部门列表（启用的部门，构建树形结构供前端缩进树展示）
        List<SysDept> deptList = sysDeptMapper.selectList(
                new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getStatus, "0")
                        .orderByAsc(SysDept::getParentId, SysDept::getOrderNum)
        );
        List<Map<String, Object>> deptTree = buildDeptTree(deptList);
        options.put("departments", deptTree);

        // 2. 岗位类别树（启用的类别，构建嵌套树形结构）
        List<JobCategory> categoryList = jobCategoryMapper.selectList(
                new LambdaQueryWrapper<JobCategory>()
                        .eq(JobCategory::getStatus, "1")
                        .orderByAsc(JobCategory::getParentId, JobCategory::getOrderNum)
        );
        // 构建树形结构：先找顶级节点，再递归挂载子节点
        List<JobCategory> categoryTree = new ArrayList<>();
        Map<Long, List<JobCategory>> parentMap = new LinkedHashMap<>();
        for (JobCategory cat : categoryList) {
            Long parentId = cat.getParentId() != null ? cat.getParentId() : 0L;
            parentMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(cat);
        }
        // 挂载子节点
        List<JobCategory> roots = parentMap.getOrDefault(0L, Collections.emptyList());
        for (JobCategory root : roots) {
            attachChildrenPortal(root, parentMap);
            categoryTree.add(root);
        }
        options.put("categories", categoryTree);

        // 3. 工作地点列表（来自 work_location 字典，含 label 中文名 + value 码值）
        List<SysDictData> dictLocations = dictDataService.selectDictDataByType("work_location");
        List<Map<String, Object>> locationList = dictLocations.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", d.getDictLabel());
            m.put("value", d.getDictValue());
            return m;
        }).collect(Collectors.toList());
        options.put("locations", locationList);

        // 4. 学历要求列表（来自 education_degree 字典，含 label 中文名 + value 码值）
        List<SysDictData> dictDegrees = dictDataService.selectDictDataByType("education_degree");
        List<Map<String, Object>> degreeList = dictDegrees.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", d.getDictLabel());
            m.put("value", d.getDictValue());
            return m;
        }).collect(Collectors.toList());
        options.put("degreeRequirements", degreeList);

        return options;
    }

    /** 递归挂载子节点到父类别 */
    private void attachChildrenPortal(JobCategory parent, Map<Long, List<JobCategory>> parentMap) {
        List<JobCategory> children = parentMap.getOrDefault(parent.getCategoryId(), Collections.emptyList());
        if (!children.isEmpty()) {
            parent.setChildren(children);
            for (JobCategory child : children) {
                attachChildrenPortal(child, parentMap);
            }
        }
    }

    /**
     * 构建部门树（Map 结构：deptId/deptName/parentId/children）
     * <p>供学生端部门筛选缩进树渲染使用。SysDept 无 children 字段，用 Map 表达层级。</p>
     */
    private List<Map<String, Object>> buildDeptTree(List<SysDept> deptList) {
        // 先转成 Map 节点
        List<Map<String, Object>> nodes = deptList.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("deptId", d.getDeptId());
            m.put("deptName", d.getDeptName());
            m.put("parentId", d.getParentId() != null ? d.getParentId() : 0L);
            m.put("children", new ArrayList<Map<String, Object>>());
            return m;
        }).collect(Collectors.toList());

        // 构建 parentId → 子节点 映射
        Map<Long, List<Map<String, Object>>> parentMap = new LinkedHashMap<>();
        for (Map<String, Object> node : nodes) {
            Long parentId = (Long) node.get("parentId");
            parentMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(node);
        }
        // 顶级节点（parentId=0）
        List<Map<String, Object>> roots = parentMap.getOrDefault(0L, Collections.emptyList());
        for (Map<String, Object> root : roots) {
            attachDeptChildrenPortal(root, parentMap);
        }
        return roots;
    }

    /** 递归挂载子节点到父部门 */
    @SuppressWarnings("unchecked")
    private void attachDeptChildrenPortal(Map<String, Object> parent, Map<Long, List<Map<String, Object>>> parentMap) {
        Long deptId = (Long) parent.get("deptId");
        List<Map<String, Object>> children = parentMap.getOrDefault(deptId, Collections.emptyList());
        if (!children.isEmpty()) {
            parent.put("children", children);
            for (Map<String, Object> child : children) {
                attachDeptChildrenPortal(child, parentMap);
            }
        }
    }
}
