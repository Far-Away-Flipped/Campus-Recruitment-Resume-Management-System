package com.atmoto.recruit.system.mapper;

import com.atmoto.recruit.system.domain.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    // BaseMapper 已提供单表 CRUD，复杂查询在 XML 中定义
}
