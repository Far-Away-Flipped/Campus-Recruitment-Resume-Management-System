package com.atmoto.recruit.system.mapper;

import com.atmoto.recruit.system.domain.SysNetworkConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 网络配置开关 Mapper
 * <p>纯 CRUD，KV 表结构与 BrandConfigMapper 风格一致。逻辑很简单，不单独建 Service 接口，
 * 由 {@code SysNetworkConfigController} 直接注入使用。</p>
 */
@Mapper
public interface SysNetworkConfigMapper extends BaseMapper<SysNetworkConfig> {
}
