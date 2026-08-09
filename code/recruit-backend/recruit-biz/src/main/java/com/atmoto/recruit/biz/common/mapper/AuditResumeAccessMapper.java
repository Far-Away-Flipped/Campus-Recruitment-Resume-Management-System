package com.atmoto.recruit.biz.common.mapper;

import com.atmoto.recruit.biz.common.domain.AuditResumeAccess;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 简历访问审计 Mapper
 */
@Mapper
public interface AuditResumeAccessMapper extends BaseMapper<AuditResumeAccess> {
}
