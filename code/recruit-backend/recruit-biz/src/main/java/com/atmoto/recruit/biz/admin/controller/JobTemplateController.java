package com.atmoto.recruit.biz.admin.controller;

import com.atmoto.recruit.biz.admin.service.JobTemplateService;
import com.atmoto.recruit.biz.common.domain.JobTemplate;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.common.core.domain.TableDataInfo;
import com.atmoto.recruit.common.core.page.PageQuery;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 岗位模板 Controller（H-003）
 * <p>管理岗位模板的增删改查，支持 HR 使用模板快速创建岗位</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/job-templates")
public class JobTemplateController {

    private final JobTemplateService jobTemplateService;

    /**
     * 分页查询岗位模板列表
     * <p>前端期望 {code:200, data:{total:N, rows:[...]}} 分页结构</p>
     */
    @GetMapping("/list")
    public AjaxResult list(JobTemplate template, PageQuery pageQuery) {
        IPage<JobTemplate> page = jobTemplateService.selectTemplatePage(template, pageQuery);
        TableDataInfo dataInfo = TableDataInfo.of(page.getTotal(), page.getRecords());
        return AjaxResult.page(dataInfo);
    }

    /**
     * 查询岗位模板详情
     */
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        JobTemplate template = jobTemplateService.selectTemplateById(id);
        return template != null ? AjaxResult.success(template) : AjaxResult.error("模板不存在");
    }

    /**
     * 新增岗位模板
     */
    @PostMapping
    public AjaxResult add(@RequestBody JobTemplate template) {
        int rows = jobTemplateService.insertTemplate(template);
        return rows > 0 ? AjaxResult.success("新增岗位模板成功") : AjaxResult.error("新增岗位模板失败");
    }

    /**
     * 修改岗位模板
     */
    @PutMapping
    public AjaxResult edit(@RequestBody JobTemplate template) {
        int rows = jobTemplateService.updateTemplate(template);
        return rows > 0 ? AjaxResult.success("修改岗位模板成功") : AjaxResult.error("修改岗位模板失败");
    }

    /**
     * 删除岗位模板
     */
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        int rows = jobTemplateService.deleteTemplateById(id);
        return rows > 0 ? AjaxResult.success("删除岗位模板成功") : AjaxResult.error("删除岗位模板失败");
    }
}
