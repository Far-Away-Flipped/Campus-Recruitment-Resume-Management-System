package com.atmoto.recruit.biz.admin.service;

import com.atmoto.recruit.biz.common.domain.JobTemplate;
import com.atmoto.recruit.common.core.page.PageQuery;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 岗位模板 Service 接口（H-003）
 * <p>管理岗位模板的增删改查，支持 HR 使用模板快速创建岗位</p>
 *
 * @author atmoto-recruit
 */
public interface JobTemplateService {

    /**
     * 查询岗位模板列表
     *
     * @param template 查询条件
     * @return 模板列表
     */
    List<JobTemplate> selectTemplateList(JobTemplate template);

    /**
     * 分页查询岗位模板列表
     *
     * @param template  查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    IPage<JobTemplate> selectTemplatePage(JobTemplate template, PageQuery pageQuery);

    /**
     * 根据ID查询岗位模板
     *
     * @param id 模板ID
     * @return 模板信息
     */
    JobTemplate selectTemplateById(Long id);

    /**
     * 新增岗位模板
     *
     * @param template 模板信息
     * @return 影响行数
     */
    int insertTemplate(JobTemplate template);

    /**
     * 修改岗位模板
     *
     * @param template 模板信息
     * @return 影响行数
     */
    int updateTemplate(JobTemplate template);

    /**
     * 删除岗位模板（逻辑删除）
     *
     * @param id 模板ID
     * @return 影响行数
     */
    int deleteTemplateById(Long id);
}
