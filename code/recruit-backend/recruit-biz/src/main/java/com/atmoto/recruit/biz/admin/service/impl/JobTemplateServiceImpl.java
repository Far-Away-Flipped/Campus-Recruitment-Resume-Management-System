package com.atmoto.recruit.biz.admin.service.impl;

import com.atmoto.recruit.biz.admin.service.JobTemplateService;
import com.atmoto.recruit.biz.common.domain.JobTemplate;
import com.atmoto.recruit.biz.common.mapper.JobTemplateMapper;
import com.atmoto.recruit.common.core.page.PageQuery;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 岗位模板 Service 实现（H-003）
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobTemplateServiceImpl implements JobTemplateService {

    private final JobTemplateMapper jobTemplateMapper;

    @Override
    public List<JobTemplate> selectTemplateList(JobTemplate template) {
        LambdaQueryWrapper<JobTemplate> wrapper = new LambdaQueryWrapper<>();
        if (template != null && template.getTemplateName() != null && !template.getTemplateName().isEmpty()) {
            wrapper.like(JobTemplate::getTemplateName, template.getTemplateName());
        }
        wrapper.orderByDesc(JobTemplate::getCreateTime);
        return jobTemplateMapper.selectList(wrapper);
    }

    @Override
    public IPage<JobTemplate> selectTemplatePage(JobTemplate template, PageQuery pageQuery) {
        // 构建分页对象
        Page<JobTemplate> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());

        // 构建查询条件
        LambdaQueryWrapper<JobTemplate> wrapper = new LambdaQueryWrapper<>();
        if (template != null && template.getTemplateName() != null && !template.getTemplateName().isEmpty()) {
            wrapper.like(JobTemplate::getTemplateName, template.getTemplateName());
        }
        wrapper.orderByDesc(JobTemplate::getCreateTime);

        return jobTemplateMapper.selectPage(page, wrapper);
    }

    @Override
    public JobTemplate selectTemplateById(Long id) {
        return jobTemplateMapper.selectById(id);
    }

    @Override
    public int insertTemplate(JobTemplate template) {
        return jobTemplateMapper.insert(template);
    }

    @Override
    public int updateTemplate(JobTemplate template) {
        return jobTemplateMapper.updateById(template);
    }

    @Override
    public int deleteTemplateById(Long id) {
        return jobTemplateMapper.deleteById(id);
    }
}
