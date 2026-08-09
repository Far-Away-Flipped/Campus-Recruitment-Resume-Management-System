package com.atmoto.recruit.biz.admin.service.impl;

import com.atmoto.recruit.biz.admin.service.NotifyTemplateService;
import com.atmoto.recruit.biz.common.domain.NotifyTemplate;
import com.atmoto.recruit.biz.common.mapper.NotifyTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 通知模板 Service 实现
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyTemplateServiceImpl implements NotifyTemplateService {

    private final NotifyTemplateMapper templateMapper;

    @Override
    public List<NotifyTemplate> selectTemplateList(NotifyTemplate template) {
        LambdaQueryWrapper<NotifyTemplate> wrapper = new LambdaQueryWrapper<>();
        if (template.getTemplateName() != null && !template.getTemplateName().isEmpty()) {
            wrapper.like(NotifyTemplate::getTemplateName, template.getTemplateName());
        }
        if (template.getTemplateCode() != null && !template.getTemplateCode().isEmpty()) {
            wrapper.like(NotifyTemplate::getTemplateCode, template.getTemplateCode());
        }
        if (template.getChannel() != null && !template.getChannel().isEmpty()) {
            wrapper.eq(NotifyTemplate::getChannel, template.getChannel());
        }
        if (template.getStatus() != null && !template.getStatus().isEmpty()) {
            wrapper.eq(NotifyTemplate::getStatus, template.getStatus());
        }
        wrapper.orderByAsc(NotifyTemplate::getId);
        return templateMapper.selectList(wrapper);
    }

    @Override
    public NotifyTemplate selectTemplateById(Long id) {
        return templateMapper.selectById(id);
    }

    @Override
    public NotifyTemplate selectTemplateByCode(String templateCode) {
        return templateMapper.selectByTemplateCode(templateCode);
    }

    @Override
    public int insertTemplate(NotifyTemplate template) {
        return templateMapper.insert(template);
    }

    @Override
    public int updateTemplate(NotifyTemplate template) {
        return templateMapper.updateById(template);
    }

    @Override
    public int deleteTemplateByIds(Long[] ids) {
        return templateMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public boolean checkTemplateCodeUnique(NotifyTemplate template) {
        Long id = template.getId() == null ? -1L : template.getId();
        NotifyTemplate exist = templateMapper.selectOne(
                new LambdaQueryWrapper<NotifyTemplate>()
                        .eq(NotifyTemplate::getTemplateCode, template.getTemplateCode())
                        .last("LIMIT 1")
        );
        return exist == null || exist.getId().equals(id);
    }
}
