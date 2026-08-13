package com.atmoto.recruit.system.service.impl;

import com.atmoto.recruit.common.core.page.PageQuery;
import com.atmoto.recruit.system.domain.SysRole;
import com.atmoto.recruit.system.mapper.SysRoleMapper;
import com.atmoto.recruit.system.service.ISysRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 系统角色 Service 实现
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements ISysRoleService {

    private final SysRoleMapper roleMapper;

    @Override
    public SysRole selectRoleById(Long roleId) {
        return roleMapper.selectById(roleId);
    }

    @Override
    public List<SysRole> selectRoleList(SysRole role) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (role.getRoleName() != null && !role.getRoleName().isEmpty()) {
            wrapper.like(SysRole::getRoleName, role.getRoleName());
        }
        if (role.getRoleKey() != null && !role.getRoleKey().isEmpty()) {
            wrapper.like(SysRole::getRoleKey, role.getRoleKey());
        }
        if (role.getStatus() != null && !role.getStatus().isEmpty()) {
            wrapper.eq(SysRole::getStatus, role.getStatus());
        }
        wrapper.orderByAsc(SysRole::getRoleSort);
        return roleMapper.selectList(wrapper);
    }

    @Override
    public IPage<SysRole> selectRolePage(SysRole role, PageQuery pageQuery) {
        Page<SysRole> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (role.getRoleName() != null && !role.getRoleName().isEmpty()) {
            wrapper.like(SysRole::getRoleName, role.getRoleName());
        }
        if (role.getRoleKey() != null && !role.getRoleKey().isEmpty()) {
            wrapper.like(SysRole::getRoleKey, role.getRoleKey());
        }
        if (role.getStatus() != null && !role.getStatus().isEmpty()) {
            wrapper.eq(SysRole::getStatus, role.getStatus());
        }
        wrapper.orderByAsc(SysRole::getRoleSort);
        return roleMapper.selectPage(page, wrapper);
    }

    @Override
    public List<SysRole> selectRoleAll() {
        return roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getStatus, "0")
                        .orderByAsc(SysRole::getRoleSort)
        );
    }

    @Override
    public int insertRole(SysRole role) {
        return roleMapper.insert(role);
    }

    @Override
    public int updateRole(SysRole role) {
        return roleMapper.updateById(role);
    }

    @Override
    public int deleteRoleByIds(Long[] roleIds) {
        return roleMapper.deleteBatchIds(Arrays.asList(roleIds));
    }

    @Override
    public boolean checkRoleNameUnique(SysRole role) {
        Long roleId = role.getRoleId() == null ? -1L : role.getRoleId();
        SysRole exist = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleName, role.getRoleName())
                        .last("LIMIT 1")
        );
        return exist == null || exist.getRoleId().equals(roleId);
    }

    @Override
    public boolean checkRoleKeyUnique(SysRole role) {
        Long roleId = role.getRoleId() == null ? -1L : role.getRoleId();
        SysRole exist = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleKey, role.getRoleKey())
                        .last("LIMIT 1")
        );
        return exist == null || exist.getRoleId().equals(roleId);
    }

    @Override
    public List<String> selectRoleKeysByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return roleMapper.selectRoleKeysByUserId(userId);
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> selectRolesByUserIds(java.util.List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return roleMapper.selectRolesByUserIds(userIds);
    }

    @Override
    public Long selectRoleIdByRoleKey(String roleKey) {
        return roleMapper.selectRoleIdByRoleKey(roleKey);
    }
}
