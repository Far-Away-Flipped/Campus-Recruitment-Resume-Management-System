package com.atmoto.recruit.biz.admin.controller;

import com.atmoto.recruit.biz.admin.service.ReportService;
import com.atmoto.recruit.biz.admin.vo.ReportDataVO;
import com.atmoto.recruit.biz.admin.vo.TrendReportVO;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.atmoto.recruit.framework.security.context.AdminUserHolder;
import com.atmoto.recruit.system.domain.SysUser;
import com.atmoto.recruit.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 报表 Controller
 * <p>HR端数据统计报表：投递趋势、岗位排行、学校分布、学历分布、渠道来源分布。
 * 一期口径仅到筛选环节，所有报表查询均含数据范围约束。</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reports")
public class ReportController {

    private final ReportService reportService;
    private final ISysUserService sysUserService;

    /**
     * 投递总量趋势
     * <p>按日期分组统计每日投递数，返回折线图所需数据</p>
     *
     * @param startDate 起始日期（可空，格式 yyyy-MM-dd）
     * @param endDate   截止日期（可空，格式 yyyy-MM-dd）
     */
    @GetMapping("/apply-trend")
    public AjaxResult applyTrend(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                  @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Long ownerUserId = resolveOwnerUserId();
        List<TrendReportVO> result = reportService.getApplyTrend(startDate, endDate, ownerUserId);
        return AjaxResult.success(result);
    }

    /**
     * 岗位投递排行
     * <p>按岗位分组统计投递数，降序返回前N条</p>
     *
     * @param topN 返回前N条（默认10）
     */
    @GetMapping("/job-ranking")
    public AjaxResult jobRanking(@RequestParam(defaultValue = "10") int topN) {
        if (topN <= 0 || topN > 100) {
            return AjaxResult.error("topN 必须在 1~100 之间");
        }
        Long ownerUserId = resolveOwnerUserId();
        List<ReportDataVO> result = reportService.getJobRanking(topN, ownerUserId);
        return AjaxResult.success(result);
    }

    /**
     * 学校分布
     * <p>按快照学校字段分组统计投递数</p>
     */
    @GetMapping("/school-distribution")
    public AjaxResult schoolDistribution() {
        Long ownerUserId = resolveOwnerUserId();
        List<ReportDataVO> result = reportService.getSchoolDistribution(ownerUserId);
        return AjaxResult.success(result);
    }

    /**
     * 学历分布
     * <p>按快照学历字段分组统计投递数</p>
     */
    @GetMapping("/degree-distribution")
    public AjaxResult degreeDistribution() {
        Long ownerUserId = resolveOwnerUserId();
        List<ReportDataVO> result = reportService.getDegreeDistribution(ownerUserId);
        return AjaxResult.success(result);
    }

    /**
     * 渠道来源分布
     * <p>按 source 字段分组统计投递数，支持可空日期范围筛选</p>
     *
     * @param startDate 起始日期（可空，格式 yyyy-MM-dd）
     * @param endDate   截止日期（可空，格式 yyyy-MM-dd）
     */
    @GetMapping({"/source-distribution", "/channel-source"})
    public AjaxResult sourceDistribution(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Long ownerUserId = resolveOwnerUserId();
        List<ReportDataVO> result = reportService.getSourceDistribution(startDate, endDate, ownerUserId);
        return AjaxResult.success(result);
    }

    // ────────────────── 内部工具方法 ──────────────────

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Long userId = AdminUserHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 判断当前用户是否拥有全部数据权限
     * <p>sys_admin 类型用户拥有全部数据权限，可查看所有岗位的投递数据</p>
     */
    private boolean hasAllDataScope() {
        Long userId = AdminUserHolder.getUserId();
        if (userId == null) {
            return false;
        }
        try {
            SysUser sysUser = sysUserService.selectUserById(userId);
            if (sysUser != null && "sys_admin".equals(sysUser.getUserType())) {
                return true;
            }
        } catch (Exception e) {
            log.warn("查询用户数据权限失败：userId={}", userId, e);
        }
        return false;
    }

    /**
     * 解析数据范围 ownerUserId
     * <p>有全部权限则返回null（查全部），否则返回当前用户ID（仅查自己负责的岗位）</p>
     */
    private Long resolveOwnerUserId() {
        return hasAllDataScope() ? null : getCurrentUserId();
    }
}
