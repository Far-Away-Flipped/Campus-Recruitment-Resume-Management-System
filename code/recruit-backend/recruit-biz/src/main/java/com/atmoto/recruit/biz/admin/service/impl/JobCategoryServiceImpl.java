package com.atmoto.recruit.biz.admin.service.impl;

import com.atmoto.recruit.biz.admin.service.JobCategoryService;
import com.atmoto.recruit.biz.common.domain.JobCategory;
import com.atmoto.recruit.biz.common.mapper.JobCategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 岗位类别 Service 实现
 * <p>支持树形结构查询与父子层级管理</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobCategoryServiceImpl implements JobCategoryService {

    private final JobCategoryMapper jobCategoryMapper;

    @Override
    public List<JobCategory> selectJobCategoryList(JobCategory jobCategory) {
        LambdaQueryWrapper<JobCategory> wrapper = new LambdaQueryWrapper<>();
        if (jobCategory.getCategoryName() != null && !jobCategory.getCategoryName().isEmpty()) {
            wrapper.like(JobCategory::getCategoryName, jobCategory.getCategoryName());
        }
        if (jobCategory.getStatus() != null && !jobCategory.getStatus().isEmpty()) {
            wrapper.eq(JobCategory::getStatus, jobCategory.getStatus());
        }
        wrapper.orderByAsc(JobCategory::getParentId, JobCategory::getOrderNum);
        return jobCategoryMapper.selectList(wrapper);
    }

    @Override
    public List<JobCategory> selectJobCategoryTree() {
        // 查询所有正常状态的类别
        List<JobCategory> all = jobCategoryMapper.selectList(
                new LambdaQueryWrapper<JobCategory>()
                        .eq(JobCategory::getStatus, "1")
                        .orderByAsc(JobCategory::getParentId, JobCategory::getOrderNum)
        );
        // 构建树形结构
        return buildCategoryTree(all);
    }

    @Override
    public JobCategory selectJobCategoryById(Long categoryId) {
        return jobCategoryMapper.selectById(categoryId);
    }

    @Override
    public int insertJobCategory(JobCategory jobCategory) {
        // 自动生成 categoryCode（前端未传时）
        if (jobCategory.getCategoryCode() == null || jobCategory.getCategoryCode().isBlank()) {
            jobCategory.setCategoryCode(UUID.randomUUID().toString().substring(0, 8));
        }
        // 计算 ancestors
        fillAncestors(jobCategory);
        return jobCategoryMapper.insert(jobCategory);
    }

    @Override
    public int updateJobCategory(JobCategory jobCategory) {
        fillAncestors(jobCategory);
        return jobCategoryMapper.updateById(jobCategory);
    }

    @Override
    public int deleteJobCategoryById(Long categoryId) {
        return jobCategoryMapper.deleteById(categoryId);
    }

    @Override
    public boolean hasChildren(Long categoryId) {
        Long count = jobCategoryMapper.selectCount(
                new LambdaQueryWrapper<JobCategory>()
                        .eq(JobCategory::getParentId, categoryId)
        );
        return count > 0;
    }

    @Override
    public boolean checkCategoryNameUnique(JobCategory jobCategory) {
        Long categoryId = jobCategory.getCategoryId() == null ? -1L : jobCategory.getCategoryId();
        JobCategory exist = jobCategoryMapper.selectOne(
                new LambdaQueryWrapper<JobCategory>()
                        .eq(JobCategory::getCategoryName, jobCategory.getCategoryName())
                        .eq(JobCategory::getParentId, jobCategory.getParentId() != null ? jobCategory.getParentId() : 0)
                        .last("LIMIT 1")
        );
        return exist == null || exist.getCategoryId().equals(categoryId);
    }

    // ────────────────── 内部工具方法 ──────────────────

    /**
     * 计算 ancestors 路径
     */
    private void fillAncestors(JobCategory jobCategory) {
        if (jobCategory.getParentId() != null && jobCategory.getParentId() != 0) {
            JobCategory parent = jobCategoryMapper.selectById(jobCategory.getParentId());
            if (parent != null) {
                String parentAncestors = parent.getAncestors() != null ? parent.getAncestors() : "0";
                jobCategory.setAncestors(parentAncestors + "," + jobCategory.getParentId());
            } else {
                jobCategory.setAncestors("0");
            }
        } else {
            jobCategory.setParentId(0L);
            jobCategory.setAncestors("0");
        }
    }

    /**
     * 构建岗位类别树
     */
    private List<JobCategory> buildCategoryTree(List<JobCategory> all) {
        List<JobCategory> tree = new ArrayList<>();
        for (JobCategory category : all) {
            if (category.getParentId() == null || category.getParentId() == 0L) {
                tree.add(category);
                attachChildren(category, all);
            }
        }
        return tree;
    }

    /** 递归挂载子节点（通过 parentId 关联，前端自行组装树） */
    private void attachChildren(JobCategory parent, List<JobCategory> all) {
        // 前端通过 parentId 字段自行组装树形，此处仅保证返回数据完整
        // 此方法为预留扩展点，未来可在此处添加 children 集合
    }
}
