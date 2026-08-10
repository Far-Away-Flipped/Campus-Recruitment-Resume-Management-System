package com.atmoto.recruit.biz.admin.controller;

import com.atmoto.recruit.biz.admin.service.JobCategoryService;
import com.atmoto.recruit.biz.common.domain.JobCategory;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.common.core.page.PageQuery;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 岗位类别 Controller（A-002）
 * <p>管理岗位类别树形结构：三大序列及其子类别</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/admin/jobCategory", "/api/admin/job-categories"})
public class JobCategoryController {

    private final JobCategoryService jobCategoryService;

    /**
     * 查询岗位类别树
     */
    @GetMapping("/tree")
    public AjaxResult tree() {
        List<JobCategory> tree = jobCategoryService.selectJobCategoryTree();
        return AjaxResult.success(tree);
    }

    /**
     * 查询岗位类别列表（平铺）
     */
    @GetMapping("/list")
    public AjaxResult list(JobCategory jobCategory) {
        List<JobCategory> list = jobCategoryService.selectJobCategoryList(jobCategory);
        return AjaxResult.success(list);
    }

    /**
     * 查询岗位类别详情
     */
    @GetMapping("/{categoryId}")
    public AjaxResult getInfo(@PathVariable Long categoryId) {
        JobCategory category = jobCategoryService.selectJobCategoryById(categoryId);
        return AjaxResult.success(category);
    }

    /**
     * 新增岗位类别
     */
    @PostMapping
    public AjaxResult add(@RequestBody JobCategory jobCategory) {
        // 校验名称唯一性
        if (!jobCategoryService.checkCategoryNameUnique(jobCategory)) {
            return AjaxResult.error("岗位类别名称已存在");
        }
        int rows = jobCategoryService.insertJobCategory(jobCategory);
        return rows > 0 ? AjaxResult.success("新增岗位类别成功") : AjaxResult.error("新增岗位类别失败");
    }

    /**
     * 修改岗位类别
     */
    @PutMapping
    public AjaxResult edit(@RequestBody JobCategory jobCategory) {
        // 校验名称唯一性
        if (!jobCategoryService.checkCategoryNameUnique(jobCategory)) {
            return AjaxResult.error("岗位类别名称已存在");
        }
        int rows = jobCategoryService.updateJobCategory(jobCategory);
        return rows > 0 ? AjaxResult.success("修改岗位类别成功") : AjaxResult.error("修改岗位类别失败");
    }

    /**
     * 切换岗位类别启用/禁用状态
     */
    @PutMapping("/{categoryId}/status")
    public AjaxResult toggleStatus(@PathVariable Long categoryId,
                                   @RequestParam String status) {
        if (!"0".equals(status) && !"1".equals(status)) {
            return AjaxResult.error("状态值仅支持 0（禁用）或 1（启用）");
        }
        JobCategory category = jobCategoryService.selectJobCategoryById(categoryId);
        if (category == null) {
            return AjaxResult.error("岗位类别不存在");
        }
        category.setStatus(status);
        int rows = jobCategoryService.updateJobCategory(category);
        String action = "1".equals(status) ? "启用" : "禁用";
        return rows > 0 ? AjaxResult.success(action + "成功") : AjaxResult.error(action + "失败");
    }

    /**
     * 删除岗位类别
     */
    @DeleteMapping("/{categoryId}")
    public AjaxResult remove(@PathVariable Long categoryId) {
        // 存在子类别时不允许删除
        if (jobCategoryService.hasChildren(categoryId)) {
            return AjaxResult.error("该类别下存在子类别，无法删除");
        }
        int rows = jobCategoryService.deleteJobCategoryById(categoryId);
        return rows > 0 ? AjaxResult.success("删除岗位类别成功") : AjaxResult.error("删除岗位类别失败");
    }
}
