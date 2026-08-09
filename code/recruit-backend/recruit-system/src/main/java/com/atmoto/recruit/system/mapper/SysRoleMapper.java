package com.atmoto.recruit.system.mapper;

import com.atmoto.recruit.system.domain.SysRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /** 根据用户ID查询其拥有的所有角色的role_key（仅取正常状态status='0'的角色，停用角色不参与权限判定） */
    @Select("SELECT DISTINCT r.role_key FROM sys_role r " +
            "LEFT JOIN sys_user_role ur ON r.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = '0'")
    List<String> selectRoleKeysByUserId(Long userId);
}