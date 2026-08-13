package com.atmoto.recruit.system.mapper;

import com.atmoto.recruit.system.domain.SysRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /** 根据用户ID查询其拥有的所有角色的role_key（仅取正常状态status='0'且未删除的角色，停用/已删除角色不参与权限判定） */
    @Select("SELECT DISTINCT r.role_key FROM sys_role r " +
            "LEFT JOIN sys_user_role ur ON r.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = '0' AND r.del_flag = '0'")
    List<String> selectRoleKeysByUserId(Long userId);

    /** 批量查询多个用户关联的角色（仅正常状态且未删除的角色），返回 user_id/role_id/role_name 三列 */
    @Select("<script>SELECT ur.user_id, r.role_id, r.role_name FROM sys_user_role ur "
            + "JOIN sys_role r ON ur.role_id = r.role_id "
            + "WHERE r.status='0' AND r.del_flag='0' AND ur.user_id IN "
            + "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<java.util.Map<String, Object>> selectRolesByUserIds(@org.apache.ibatis.annotations.Param("userIds") List<Long> userIds);

    /** 批量给用户绑定角色（ON DUPLICATE KEY 幂等，避免重复绑定报错） */
    @org.apache.ibatis.annotations.Insert("<script>INSERT INTO sys_user_role (user_id, role_id) VALUES "
            + "<foreach collection='roleIds' item='rid' separator=','>(#{userId}, #{rid})</foreach> "
            + "ON DUPLICATE KEY UPDATE role_id = role_id</script>")
    int insertUserRole(@org.apache.ibatis.annotations.Param("userId") Long userId,
                       @org.apache.ibatis.annotations.Param("roleIds") List<Long> roleIds);

    /** 删除指定用户的全部角色绑定（物理删除，用于角色同步前的清理） */
    @org.apache.ibatis.annotations.Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteUserRolesByUserId(@org.apache.ibatis.annotations.Param("userId") Long userId);

    /** 按 role_key 查询角色ID（仅未删除角色） */
    @org.apache.ibatis.annotations.Select("SELECT role_id FROM sys_role WHERE role_key = #{roleKey} AND del_flag = '0' LIMIT 1")
    Long selectRoleIdByRoleKey(@org.apache.ibatis.annotations.Param("roleKey") String roleKey);
}