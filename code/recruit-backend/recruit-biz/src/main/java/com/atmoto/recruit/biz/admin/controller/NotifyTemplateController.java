package com.atmoto.recruit.biz.admin.controller;

import com.atmoto.recruit.biz.admin.service.NotifyTemplateService;
import com.atmoto.recruit.biz.common.domain.NotifyTemplate;
import com.atmoto.recruit.biz.common.security.AdminRoleGuard;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知模板 Controller（A-001-T）
 * <p>管理通知模板的增删改查</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notifyTemplate")
public class NotifyTemplateController {

    private final NotifyTemplateService templateService;
    private final AdminRoleGuard adminRoleGuard;

    /**
     * 查询模板列表
     */
    @GetMapping("/list")
    public AjaxResult list(NotifyTemplate template) {
        adminRoleGuard.requireDirector();
        List<NotifyTemplate> list = templateService.selectTemplateList(template);
        return AjaxResult.success(list);
    }

    /**
     * 查询模板详情
     */
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        adminRoleGuard.requireDirector();
        NotifyTemplate template = templateService.selectTemplateById(id);
        return AjaxResult.success(template);
    }

    /**
     * 根据模板编码查询模板
     */
    @GetMapping("/code/{templateCode}")
    public AjaxResult getByCode(@PathVariable String templateCode) {
        adminRoleGuard.requireDirector();
        NotifyTemplate template = templateService.selectTemplateByCode(templateCode);
        return template != null ? AjaxResult.success(template) : AjaxResult.error("模板不存在");
    }

    /**
     * 新增模板
     */
    @PostMapping
    public AjaxResult add(@RequestBody NotifyTemplate template) {
        adminRoleGuard.requireDirector();
        if (!templateService.checkTemplateCodeUnique(template)) {
            return AjaxResult.error("模板编码已存在");
        }
        int rows = templateService.insertTemplate(template);
        return rows > 0 ? AjaxResult.success("新增模板成功") : AjaxResult.error("新增模板失败");
    }

    /**
     * 修改模板
     */
    @PutMapping
    public AjaxResult edit(@RequestBody NotifyTemplate template) {
        adminRoleGuard.requireDirector();
        if (!templateService.checkTemplateCodeUnique(template)) {
            return AjaxResult.error("模板编码已存在");
        }
        int rows = templateService.updateTemplate(template);
        return rows > 0 ? AjaxResult.success("修改模板成功") : AjaxResult.error("修改模板失败");
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        adminRoleGuard.requireDirector();
        int rows = templateService.deleteTemplateByIds(ids);
        return rows > 0 ? AjaxResult.success("删除模板成功") : AjaxResult.error("删除模板失败");
    }
}
