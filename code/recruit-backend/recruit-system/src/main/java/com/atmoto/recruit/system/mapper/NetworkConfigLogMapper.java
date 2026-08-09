package com.atmoto.recruit.system.mapper;

import com.atmoto.recruit.system.domain.NetworkConfigLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 网络配置变更审计 Mapper
 * <p>纯 CRUD（实际只用到 insert 与 selectPage/selectList），对应表 audit_network_config。</p>
 */
@Mapper
public interface NetworkConfigLogMapper extends BaseMapper<NetworkConfigLog> {
}
