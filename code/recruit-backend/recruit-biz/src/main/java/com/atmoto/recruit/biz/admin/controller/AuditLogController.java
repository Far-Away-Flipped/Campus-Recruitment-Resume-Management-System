package com.atmoto.recruit.biz.admin.controller;

import com.atmoto.recruit.biz.common.domain.AuditResumeAccess;
import com.atmoto.recruit.biz.common.mapper.AuditResumeAccessMapper;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.common.core.domain.TableDataInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 操作审计日志查询控制器
 */
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditResumeAccessMapper auditResumeAccessMapper;

    /** 分页查询审计日志 */
    @GetMapping("/list")
    public AjaxResult list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String operatorName) {

        LambdaQueryWrapper<AuditResumeAccess> wrapper = new LambdaQueryWrapper<>();
        if (operationType != null && !operationType.isEmpty()) {
            wrapper.eq(AuditResumeAccess::getOperationType, operationType);
        }
        if (operatorName != null && !operatorName.isEmpty()) {
            wrapper.like(AuditResumeAccess::getOperatorName, operatorName);
        }
        wrapper.orderByDesc(AuditResumeAccess::getCreateTime);

        Page<AuditResumeAccess> page = new Page<>(pageNum, pageSize);
        Page<AuditResumeAccess> result = auditResumeAccessMapper.selectPage(page, wrapper);

        return AjaxResult.page(TableDataInfo.of(
            (int) result.getTotal(), result.getRecords()));
    }

    /** 审计日志操作类型列表 */
    @GetMapping("/types")
    public AjaxResult types() {
        return AjaxResult.success(new String[]{
            "VIEW_RESUME", "DOWNLOAD_ATTACHMENT", "EXPORT_RESUME",
            "SCREEN_PASS", "SCREEN_ELIMINATE", "BATCH_SCREEN",
            "ADD_REMARK", "STATUS_CHANGE", "VIEW_AUDIT_LOG"
        });
    }
}
