package com.atmoto.recruit.system.service;

import com.atmoto.recruit.system.domain.SysMenu;

import java.util.List;

/**
 * 系统菜单 Service 接口
 * <p>管理菜单树、权限标识，支持按用户ID查询可见菜单</p>
 *
 * @author atmoto-recruit
 */
public interface ISysMenuService {

    /**
     * 根据用户ID查询菜单树（用于前端动态路由渲染）
     *
     * @param userId 用户ID
     * @return 菜单树列表
     */
    List<SysMenu> selectMenuTreeByUserId(Long userId);

    /**
     * 查询全部菜单列表
     *
     * @param menu 查询条件
     * @return 菜单列表
     */
    List<SysMenu> selectMenuList(SysMenu menu);

    /**
     * 根据用户ID查询菜单列表（含条件过滤）
     *
     * @param menu   查询条件
     * @param userId 用户ID
     * @return 菜单列表
     */
    List<SysMenu> selectMenuList(SysMenu menu, Long userId);

    /**
     * 根据菜单ID查询菜单详情
     *
     * @param menuId 菜单ID
     * @return 菜单信息
     */
    SysMenu selectMenuById(Long menuId);

    /**
     * 根据用户ID查询其所拥有的菜单权限
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    List<SysMenu> selectMenusByUserId(Long userId);

    /**
     * 新增菜单
     *
     * @param menu 菜单信息
     * @return 影响行数
     */
    int insertMenu(SysMenu menu);

    /**
     * 修改菜单
     *
     * @param menu 菜单信息
     * @return 影响行数
     */
    int updateMenu(SysMenu menu);

    /**
     * 删除菜单（含子菜单）
     *
     * @param menuId 菜单ID
     * @return 影响行数
     */
    int deleteMenuById(Long menuId);

    /**
     * 校验菜单名称是否唯一
     *
     * @param menu 菜单信息
     * @return true=唯一
     */
    boolean checkMenuNameUnique(SysMenu menu);
}
