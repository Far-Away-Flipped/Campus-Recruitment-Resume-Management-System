package com.atmoto.recruit.biz.portal.controller;

import com.atmoto.recruit.biz.common.domain.Application;
import com.atmoto.recruit.biz.common.service.ApplicationService;
import com.atmoto.recruit.biz.portal.dto.SubmitApplicationRequest;
import com.atmoto.recruit.biz.portal.service.PortalApplicationService;
import com.atmoto.recruit.biz.portal.vo.ApplicationDetailVo;
import com.atmoto.recruit.biz.portal.vo.ApplicationListVo;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.common.core.domain.TableDataInfo;
import com.atmoto.recruit.common.core.page.PageQuery;
import com.atmoto.recruit.framework.security.context.PortalUserHolder;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 门户端投递 Controller（M4 投递流程）
 * <p>
 * 前缀：/api/portal/applications，提供投递、投递列表、投递详情功能。
 * 铁律：studentId 一律从 PortalUserHolder 获取，绝不从请求参数传入。
 * </p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portal/applications")
public class PortalApplicationController {

    private final ApplicationService applicationService;
    private final PortalApplicationService portalApplicationService;

    /**
     * 投递岗位
     * <p>
     * 校验链：岗位存在且已发布 → 资料完整性 → 防重复投递 → 乐观锁。
     * 事务内：生成快照 → 创建投递记录 → 记录状态历史 → 更新岗位计数 → 异步通知。
     * </p>
     *
     * @param request 投递请求体，含 jobId 和 source
     * @return success + "投递成功"
     */
    @PostMapping("/submit")
    public AjaxResult submit(@Validated @RequestBody SubmitApplicationRequest request) {
        Long studentId = PortalUserHolder.get();
        Application application = applicationService.submitApplication(
                studentId, request.getJobId(), request.getSource(), request.getSourceDetail(), request.getFileId());
        return AjaxResult.success("投递成功", application.getApplicationId());
    }

    /**
     * 我的投递列表（分页）
     * <p>
     * 返回当前学生的所有投递记录，含岗位名称、部门名称、状态、投递时间。
     * 按投递时间降序排列。
     * </p>
     *
     * @param pageQuery 分页参数
     * @return 分页投递列表
     */
    @GetMapping("/my")
    public AjaxResult myApplications(PageQuery pageQuery) {
        Long studentId = PortalUserHolder.get();
        IPage<ApplicationListVo> page = portalApplicationService.getMyApplications(studentId, pageQuery);
        TableDataInfo dataInfo = TableDataInfo.of(page.getTotal(), page.getRecords());
        return AjaxResult.page(dataInfo);
    }

    /**
     * 投递详情（含状态流转历史与快照）
     * <p>
     * 返回投递记录的完整信息，包括简历快照数据与状态变更历史。
     * 严格校验归属权：仅限投递者本人查看。
     * </p>
     *
     * @param id 投递记录ID
     * @return 投递详情 VO
     */
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        Long studentId = PortalUserHolder.get();
        ApplicationDetailVo vo = portalApplicationService.getApplicationDetail(id, studentId);
        return AjaxResult.success(vo);
    }
}
