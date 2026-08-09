package com.atmoto.recruit.biz.portal.service;

import com.atmoto.recruit.biz.common.domain.JobPosition;
import com.atmoto.recruit.biz.portal.vo.PortalJobVo;
import com.atmoto.recruit.common.core.page.PageQuery;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.Map;

/**
 * 门户端岗位浏览 Service 接口
 * <p>面向学生/求职者的岗位搜索、浏览、筛选功能</p>
 *
 * @author atmoto-recruit
 */
public interface PortalJobService {

    /**
     * 分页查询已发布岗位列表
     * <p>仅返回 status='PUBLISHED' 且未过截止日期的岗位。
     * 有关键词时使用 MySQL FULLTEXT 索引进行全文搜索，无关键词时走普通 WHERE 筛选。</p>
     *
     * @param jobPosition 筛选条件（keyword 放在 title 字段中传递）
     * @param pageQuery   分页参数
     * @return 分页结果
     */
    IPage<JobPosition> selectPublishedJobList(JobPosition jobPosition, PageQuery pageQuery);

    /**
     * 查询岗位详情（含部门名称、岗位类别名称）
     * <p>同时增加浏览量 viewCount</p>
     *
     * @param jobId 岗位ID
     * @return 岗位详情 VO
     */
    PortalJobVo selectJobDetail(Long jobId);

    /**
     * 获取筛选下拉选项
     * <p>返回：部门列表、岗位类别树、工作地点列表、学历要求列表</p>
     *
     * @return 筛选选项 Map
     */
    Map<String, Object> getFilterOptions();
}
