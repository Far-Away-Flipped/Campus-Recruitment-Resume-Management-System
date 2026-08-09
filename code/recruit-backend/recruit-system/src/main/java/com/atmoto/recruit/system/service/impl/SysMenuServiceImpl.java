package com.atmoto.recruit.system.service.impl;

import com.atmoto.recruit.system.domain.SysMenu;
import com.atmoto.recruit.system.mapper.SysMenuMapper;
import com.atmoto.recruit.system.service.ISysMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统菜单 Service 实现
 * <p>支持菜单树构建和按用户权限查询</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements ISysMenuService {

    private final SysMenuMapper menuMapper;

    @Override
    public List<SysMenu> selectMenuTreeByUserId(Long userId) {
        // 查询该用户拥有的所有菜单
        List<SysMenu> menus = selectMenusByUserId(userId);
        // 构建树形结构
        return buildMenuTree(menus);
    }

    @Override
    public List<SysMenu> selectMenuList(SysMenu menu) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        if (menu.getMenuName() != null && !menu.getMenuName().isEmpty()) {
            wrapper.like(SysMenu::getMenuName, menu.getMenuName());
        }
        if (menu.getStatus() != null && !menu.getStatus().isEmpty()) {
            wrapper.eq(SysMenu::getStatus, menu.getStatus());
        }
        wrapper.orderByAsc(SysMenu::getParentId, SysMenu::getOrderNum);
        return menuMapper.selectList(wrapper);
    }

    @Override
    public List<SysMenu> selectMenuList(SysMenu menu, Long userId) {
        // 按条件筛选用户拥有的菜单
        List<SysMenu> menus = selectMenusByUserId(userId);
        // 根据 menu 条件二次过滤
        return menus.stream().filter(m -> {
            boolean match = true;
            if (menu.getMenuName() != null && !menu.getMenuName().isEmpty()) {
                match = m.getMenuName().contains(menu.getMenuName());
            }
            if (menu.getStatus() != null && !menu.getStatus().isEmpty()) {
                match = match && menu.getStatus().equals(m.getStatus());
            }
            return match;
        }).collect(Collectors.toList());
    }

    @Override
    public SysMenu selectMenuById(Long menuId) {
        return menuMapper.selectById(menuId);
    }

    @Override
    public List<SysMenu> selectMenusByUserId(Long userId) {
        return menuMapper.selectMenusByUserId(userId);
    }

    @Override
    public int insertMenu(SysMenu menu) {
        return menuMapper.insert(menu);
    }

    @Override
    public int updateMenu(SysMenu menu) {
        return menuMapper.updateById(menu);
    }

    @Override
    public int deleteMenuById(Long menuId) {
        // 同时删除子菜单
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getMenuId, menuId)
                .or()
                .eq(SysMenu::getParentId, menuId);
        return menuMapper.delete(wrapper);
    }

    @Override
    public boolean checkMenuNameUnique(SysMenu menu) {
        Long menuId = menu.getMenuId() == null ? -1L : menu.getMenuId();
        SysMenu exist = menuMapper.selectOne(
                new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getMenuName, menu.getMenuName())
                        .eq(SysMenu::getParentId, menu.getParentId())
                        .last("LIMIT 1")
        );
        return exist == null || exist.getMenuId().equals(menuId);
    }

    /**
     * 构建菜单树结构
     * <p>从平铺列表构建父子层级，根节点 parentId=0</p>
     */
    private List<SysMenu> buildMenuTree(List<SysMenu> menus) {
        List<SysMenu> tree = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (menu.getParentId() == null || menu.getParentId() == 0L) {
                tree.add(menu);
                // 递归挂载子节点
                attachChildren(menu, menus);
            }
        }
        return tree;
    }

    /** 递归挂载子菜单 */
    private void attachChildren(SysMenu parent, List<SysMenu> allMenus) {
        List<SysMenu> children = new ArrayList<>();
        for (SysMenu menu : allMenus) {
            if (menu.getParentId() != null && menu.getParentId().equals(parent.getMenuId())) {
                children.add(menu);
                attachChildren(menu, allMenus);
            }
        }
        // 注意：SysMenu 没有 children 字段，此处仅做业务分组
        // 前端通过 parentId 自行构建树形，此处仅做排序保证
    }
}
