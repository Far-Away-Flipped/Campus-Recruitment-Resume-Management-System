package com.atmoto.recruit.biz.admin.service;

import com.atmoto.recruit.biz.common.domain.NotifyTemplate;

import java.util.List;

/**
 * 通知模板 Service 接口（A-001-T）
 * <p>管理通知模板的增删改查，支持按模板编码快速查询</p>
 *
 * @author atmoto-recruit
 */
public interface NotifyTemplateService {

    /**
     * 分页查询通知模板列表
     *
     * @param template 查询条件
     * @return 模板列表
     */
    List<NotifyTemplate> selectTemplateList(NotifyTemplate template);

    /**
     * 根据ID查询模板详情
     *
     * @param id 模板ID
     * @return 模板信息
     */
    NotifyTemplate selectTemplateById(Long id);

    /**
     * 根据模板编码查询模板（用于发送通知时匹配模板）
     *
     * @param templateCode 模板编码
     * @return 模板信息
     */
    NotifyTemplate selectTemplateByCode(String templateCode);

    /**
     * 新增模板
     *
     * @param template 模板信息
     * @return 影响行数
     */
    int insertTemplate(NotifyTemplate template);

    /**
     * 修改模板
     *
     * @param template 模板信息
     * @return 影响行数
     */
    int updateTemplate(NotifyTemplate template);

    /**
     * 批量删除模板
     *
     * @param ids 模板ID数组
     * @return 影响行数
     */
    int deleteTemplateByIds(Long[] ids);

    /**
     * 校验模板编码是否唯一
     *
     * @param template 模板信息（含 id 用于编辑时排除自身）
     * @return true=唯一
     */
    boolean checkTemplateCodeUnique(NotifyTemplate template);
}
