package com.atmoto.recruit.biz.portal.controller;

import com.atmoto.recruit.biz.common.domain.JobPosition;
import com.atmoto.recruit.biz.portal.service.PortalJobService;
import com.atmoto.recruit.biz.portal.vo.PortalJobVo;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.common.core.domain.TableDataInfo;
import com.atmoto.recruit.common.core.page.PageQuery;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 门户端岗位浏览 Controller
 * <p>面向学生/求职者的岗位搜索、浏览、筛选功能，无需登录即可访问</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portal/jobs")
public class PortalJobController {

    private final PortalJobService portalJobService;

    /**
     * 岗位浏览根路径
     * <p>防止 /api/portal/jobs 无匹配处理器返回 500</p>
     */
    @GetMapping
    public AjaxResult root() {
        return AjaxResult.success("OK");
    }

    /**
     * 分页查询已发布岗位列表
     * <p>支持关键词全文搜索 + 多维度筛选。仅返回已发布且未过截止日期的岗位。</p>
     *
     * @param jobPosition 筛选条件（title 字段同时作为关键词搜索输入）
     * @param pageQuery   分页参数
     */
    @GetMapping("/list")
    public AjaxResult list(JobPosition jobPosition, PageQuery pageQuery) {
        IPage<JobPosition> page = portalJobService.selectPublishedJobList(jobPosition, pageQuery);
        TableDataInfo dataInfo = TableDataInfo.of(page.getTotal(), page.getRecords());
        return AjaxResult.page(dataInfo);
    }

    /**
     * 查询岗位详情
     * <p>含部门名称、岗位类别名称，并自动增加浏览量</p>
     */
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        PortalJobVo vo = portalJobService.selectJobDetail(id);
        return AjaxResult.success(vo);
    }

    /**
     * 获取筛选下拉选项
     * <p>返回部门列表、岗位类别树、工作地点列表、学历要求列表，供前端筛选器渲染</p>
     */
    @GetMapping("/filter-options")
    public AjaxResult filterOptions() {
        Map<String, Object> options = portalJobService.getFilterOptions();
        return AjaxResult.success(options);
    }
}
