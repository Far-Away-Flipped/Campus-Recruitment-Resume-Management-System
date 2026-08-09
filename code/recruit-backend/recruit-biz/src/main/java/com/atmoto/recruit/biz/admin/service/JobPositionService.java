package com.atmoto.recruit.biz.admin.service;

import com.atmoto.recruit.biz.common.domain.JobPosition;
import com.atmoto.recruit.common.core.page.PageQuery;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 岗位管理 Service 接口
 * <p>管理后台岗位的增删改查、发布、下架操作</p>
 *
 * @author atmoto-recruit
 */
public interface JobPositionService {

    /**
     * 分页查询岗位列表
     * <p>支持按标题模糊搜索 + status/deptId/categoryId/location/degreeRequirement 筛选 + 动态排序（通过 PageQuery.orderByColumn/isAsc 传入）</p>
     *
     * @param jobPosition 查询条件
     * @param pageQuery   分页参数（含排序字段）
     * @return 分页结果
     */
    IPage<JobPosition> selectJobList(JobPosition jobPosition, PageQuery pageQuery);

    /**
     * 根据ID查询岗位详情
     *
     * @param jobId 岗位ID
     * @return 岗位信息
     */
    JobPosition selectJobById(Long jobId);

    /**
     * 新增岗位
     * <p>创建时默认状态为 DRAFT</p>
     *
     * @param jobPosition 岗位信息
     * @return 影响行数
     */
    int insertJob(JobPosition jobPosition);

    /**
     * 修改岗位
     *
     * @param jobPosition 岗位信息
     * @return 影响行数
     */
    int updateJob(JobPosition jobPosition);

    /**
     * 发布岗位
     * <p>仅 DRAFT 或 CLOSED 状态可发布，转为 PUBLISHED</p>
     *
     * @param jobId 岗位ID
     * @return 影响行数
     */
    int publishJob(Long jobId);

    /**
     * 下架岗位
     * <p>仅 PUBLISHED 状态可下架，转为 CLOSED</p>
     *
     * @param jobId 岗位ID
     * @return 影响行数
     */
    int offlineJob(Long jobId);

    /**
     * 删除岗位（逻辑删除）
     *
     * @param jobId 岗位ID
     * @return 影响行数
     */
    int deleteJob(Long jobId);
}
