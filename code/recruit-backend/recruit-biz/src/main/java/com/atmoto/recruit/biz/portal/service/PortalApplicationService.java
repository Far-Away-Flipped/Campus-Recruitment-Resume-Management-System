package com.atmoto.recruit.biz.portal.service;

import com.atmoto.recruit.biz.portal.vo.ApplicationDetailVo;
import com.atmoto.recruit.biz.portal.vo.ApplicationListVo;
import com.atmoto.recruit.common.core.page.PageQuery;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 门户端投递查询 Service 接口（M4 投递流程 - 学生端查询）
 * <p>面向学生/求职者的投递记录查看功能</p>
 *
 * @author atmoto-recruit
 */
public interface PortalApplicationService {

    /**
     * 查询我的投递列表（分页）
     * <p>连表 job_position 和 sys_dept 获取岗位名称与部门名称。
     * 按投递时间降序排列。</p>
     *
     * @param studentId 学生ID（来自 PortalUserHolder）
     * @param pageQuery 分页参数
     * @return 分页结果（ApplicationListVo）
     */
    IPage<ApplicationListVo> getMyApplications(Long studentId, PageQuery pageQuery);

    /**
     * 查询投递详情（含快照与状态流转历史）
     * <p>校验投递记录归属权后才返回详情，防止越权查看</p>
     *
     * @param applicationId 投递记录ID
     * @param studentId     学生ID（来自 PortalUserHolder，用于归属校验）
     * @return 投递详情 VO（含快照数据与状态历史）
     */
    ApplicationDetailVo getApplicationDetail(Long applicationId, Long studentId);
}
