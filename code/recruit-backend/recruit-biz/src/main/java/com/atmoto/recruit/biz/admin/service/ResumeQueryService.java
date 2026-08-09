package com.atmoto.recruit.biz.admin.service;

import com.atmoto.recruit.biz.admin.dto.ResumeQueryDTO;
import com.atmoto.recruit.biz.admin.vo.ResumeDetailVO;
import com.atmoto.recruit.common.core.domain.TableDataInfo;
import com.atmoto.recruit.common.core.page.PageQuery;

/**
 * 简历查询服务接口
 * <p>HR端多维筛选+分页查询简历列表</p>
 *
 * @author atmoto-recruit
 */
public interface ResumeQueryService {

    /**
     * 多维筛选分页查询简历列表
     * <p>
     * 支持：岗位ID、状态、关键字、学校、专业、学历、投递时间范围。
     * 默认数据范围约束：仅返回当前HR负责岗位下的投递。
     * </p>
     *
     * @param queryDTO  筛选条件
     * @param pageQuery 分页参数
     * @param operatorUserId 当前HR用户ID
     * @param hasAllDataScope 是否拥有全部数据权限
     * @return 分页结果
     */
    TableDataInfo selectResumeList(ResumeQueryDTO queryDTO, PageQuery pageQuery,
                                   Long operatorUserId, boolean hasAllDataScope);

    /**
     * 查询简历详情
     * <p>含学生基本资料、教育经历、当前快照、附件列表</p>
     *
     * @param applicationId 投递记录ID
     * @param operatorUserId 当前HR用户ID
     * @param hasAllDataScope 是否拥有全部数据权限
     * @return 简历详情
     */
    ResumeDetailVO selectResumeDetail(Long applicationId, Long operatorUserId, boolean hasAllDataScope);
}
