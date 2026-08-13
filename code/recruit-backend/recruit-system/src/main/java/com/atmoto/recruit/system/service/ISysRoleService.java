package com.atmoto.recruit.system.service;

import com.atmoto.recruit.common.core.page.PageQuery;
import com.atmoto.recruit.system.domain.SysRole;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 系统角色 Service 接口
 * <p>管理角色的增删改查，以及角色与菜单的关联关系</p>
 *
 * @author atmoto-recruit
 */
public interface ISysRoleService {

    /**
     * 根据角色ID查询角色
     *
     * @param roleId 角色ID
     * @return 角色信息
     */
    SysRole selectRoleById(Long roleId);

    /**
     * 分页查询角色列表
     *
     * @param role 查询条件
     * @return 角色列表
     */
    List<SysRole> selectRoleList(SysRole role);

    /**
     * 分页查询角色列表（MyBatis-Plus 分页）
     *
     * @param role      查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    IPage<SysRole> selectRolePage(SysRole role, PageQuery pageQuery);

    /**
     * 查询所有正常状态的角色
     *
     * @return 角色列表
     */
    List<SysRole> selectRoleAll();

    /**
     * 新增角色
     *
     * @param role 角色信息
     * @return 影响行数
     */
    int insertRole(SysRole role);

    /**
     * 修改角色信息
     *
     * @param role 角色信息
     * @return 影响行数
     */
    int updateRole(SysRole role);

    /**
     * 批量删除角色（逻辑删除）
     *
     * @param roleIds 角色ID数组
     * @return 影响行数
     */
    int deleteRoleByIds(Long[] roleIds);

    /**
     * 校验角色名称是否唯一
     *
     * @param role 角色（包含 roleId 用于排除自身）
     * @return true=唯一
     */
    boolean checkRoleNameUnique(SysRole role);

    /**
     * 校验角色权限字符串是否唯一
     *
     * @param role 角色（包含 roleId 用于排除自身）
     * @return true=唯一
     */
    boolean checkRoleKeyUnique(SysRole role);

    /**
     * 根据用户ID查询其拥有的所有角色的role_key集合
     * <p>仅返回status='0'（正常状态）角色的role_key，已停用角色不参与权限判定。</p>
     *
     * @param userId 用户ID
     * @return role_key集合，用户无角色或角色均已停用时返回空列表（不返回null）
     */
    List<String> selectRoleKeysByUserId(Long userId);

    /**
     * 批量查询多个用户关联的角色（user_id/role_id/role_name）
     *
     * @param userIds 用户ID列表
     * @return 角色关联行，每行含 user_id/role_id/role_name
     */
    java.util.List<java.util.Map<String, Object>> selectRolesByUserIds(java.util.List<Long> userIds);

    /**
     * 按 role_key 查询角色ID
     *
     * @param roleKey 角色权限字符
     * @return 角色ID，未找到返回 null
     */
    Long selectRoleIdByRoleKey(String roleKey);
}
