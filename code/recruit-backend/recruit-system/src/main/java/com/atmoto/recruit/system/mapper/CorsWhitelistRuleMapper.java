package com.atmoto.recruit.system.mapper;

import com.atmoto.recruit.system.domain.CorsWhitelistRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * CORS 白名单规则 Mapper
 * <p>纯 CRUD，业务逻辑（缓存/降级/查重）在 ICorsWhitelistService 中封装。</p>
 */
@Mapper
public interface CorsWhitelistRuleMapper extends BaseMapper<CorsWhitelistRule> {
}
