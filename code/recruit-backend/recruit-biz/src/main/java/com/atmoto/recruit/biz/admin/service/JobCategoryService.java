package com.atmoto.recruit.biz.admin.service;

import com.atmoto.recruit.biz.common.domain.JobCategory;

import java.util.List;

/**
 * 岗位类别 Service 接口
 * <p>管理岗位类别的树形结构，支持三大序列及其子类别</p>
 *
 * @author atmoto-recruit
 */
public interface JobCategoryService {

    /**
     * 查询岗位类别列表（平铺）
     *
     * @param jobCategory 查询条件
     * @return 岗位类别列表
     */
    List<JobCategory> selectJobCategoryList(JobCategory jobCategory);

    /**
     * 查询岗位类别树（组装父子层级）
     *
     * @return 岗位类别树
     */
    List<JobCategory> selectJobCategoryTree();

    /**
     * 根据ID查询岗位类别
     *
     * @param categoryId 类别ID
     * @return 岗位类别
     */
    JobCategory selectJobCategoryById(Long categoryId);

    /**
     * 新增岗位类别
     *
     * @param jobCategory 岗位类别
     * @return 影响行数
     */
    int insertJobCategory(JobCategory jobCategory);

    /**
     * 修改岗位类别
     *
     * @param jobCategory 岗位类别
     * @return 影响行数
     */
    int updateJobCategory(JobCategory jobCategory);

    /**
     * 删除岗位类别
     *
     * @param categoryId 类别ID
     * @return 影响行数
     */
    int deleteJobCategoryById(Long categoryId);

    /**
     * 检查是否存在子类别
     *
     * @param categoryId 类别ID
     * @return true=存在子类别
     */
    boolean hasChildren(Long categoryId);

    /**
     * 校验类别名称是否唯一
     *
     * @param jobCategory 岗位类别
     * @return true=唯一
     */
    boolean checkCategoryNameUnique(JobCategory jobCategory);
}
