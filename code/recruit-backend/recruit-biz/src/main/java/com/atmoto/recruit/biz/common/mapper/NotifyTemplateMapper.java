package com.atmoto.recruit.biz.common.mapper;

import com.atmoto.recruit.biz.common.domain.NotifyTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 通知模板 Mapper
 *
 * @author atmoto-recruit
 */
@Mapper
public interface NotifyTemplateMapper extends BaseMapper<NotifyTemplate> {

    /**
     * 根据模板编码查询模板
     */
    @Select("SELECT * FROM notify_template WHERE template_code = #{templateCode} AND status = '0' LIMIT 1")
    NotifyTemplate selectByTemplateCode(String templateCode);
}
