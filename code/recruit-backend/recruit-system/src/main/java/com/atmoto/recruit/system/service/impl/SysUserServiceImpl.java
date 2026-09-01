package com.atmoto.recruit.system.service.impl;

import com.atmoto.recruit.common.core.page.PageQuery;
import com.atmoto.recruit.system.domain.SysUser;
import com.atmoto.recruit.system.mapper.SysDeptMapper;
import com.atmoto.recruit.system.mapper.SysRoleMapper;
import com.atmoto.recruit.system.mapper.SysUserMapper;
import com.atmoto.recruit.system.service.ISysUserService;
import com.atmoto.recruit.system.util.DeptTreeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 系统用户 Service 实现
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements ISysUserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;

    @Override
    public SysUser selectUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public SysUser selectUserByUserName(String userName) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserName, userName)
                        .last("LIMIT 1")
        );
    }

    @Override
    public List<SysUser> selectUserList(SysUser user) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        // 按用户名模糊查询
        if (user.getUserName() != null && !user.getUserName().isEmpty()) {
            wrapper.like(SysUser::getUserName, user.getUserName());
        }
        // 按手机号模糊查询
        if (user.getPhonenumber() != null && !user.getPhonenumber().isEmpty()) {
            wrapper.like(SysUser::getPhonenumber, user.getPhonenumber());
        }
        // 按状态精确查询
        if (user.getStatus() != null && !user.getStatus().isEmpty()) {
            wrapper.eq(SysUser::getStatus, user.getStatus());
        }
        // 按部门ID筛选：选择父部门时，需包含其全部子部门的用户
        if (user.getDeptId() != null) {
            List<Long> deptIds = DeptTreeUtil.collectDeptAndDescendants(user.getDeptId(), deptMapper, false);
            if (!deptIds.isEmpty()) {
                wrapper.in(SysUser::getDeptId, deptIds);
            }
        }
        wrapper.orderByAsc(SysUser::getUserId);
        return userMapper.selectList(wrapper);
    }

    @Override
    public IPage<SysUser> selectUserPage(SysUser user, PageQuery pageQuery) {
        Page<SysUser> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        // 按用户名/手机号/昵称模糊查询（复用 keyword 字段）
        if (user.getUserName() != null && !user.getUserName().isEmpty()) {
            wrapper.and(w -> w
                    .like(SysUser::getUserName, user.getUserName())
                    .or()
                    .like(SysUser::getPhonenumber, user.getUserName())
                    .or()
                    .like(SysUser::getNickName, user.getUserName()));
        }
        // 按状态精确查询
        if (user.getStatus() != null && !user.getStatus().isEmpty()) {
            wrapper.eq(SysUser::getStatus, user.getStatus());
        }
        // 按部门ID筛选：选择父部门时，需包含其全部子部门的用户
        if (user.getDeptId() != null) {
            List<Long> deptIds = DeptTreeUtil.collectDeptAndDescendants(user.getDeptId(), deptMapper, false);
            if (!deptIds.isEmpty()) {
                wrapper.in(SysUser::getDeptId, deptIds);
            }
        }
        wrapper.orderByAsc(SysUser::getUserId);
        return userMapper.selectPage(page, wrapper);
    }

    @Override
    public int insertUser(SysUser user) {
        return userMapper.insert(user);
    }

    @Override
    public int updateUser(SysUser user) {
        return userMapper.updateById(user);
    }

    @Override
    public int deleteUserByIds(Long[] userIds) {
        return userMapper.deleteBatchIds(Arrays.asList(userIds));
    }

    @Override
    public int resetPwd(SysUser user) {
        // 使用 LambdaUpdateWrapper 精确更新密码字段，避免 MyBatis-Plus updateById
        // 因实体其它字段为 null 导致 SQL 生成异常或返回 0 行
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getUserId, user.getUserId());
        wrapper.set(SysUser::getPassword, user.getPassword());
        return userMapper.update(null, wrapper);
    }

    @Override
    public int updateUserStatus(SysUser user) {
        return userMapper.updateById(user);
    }

    @Override
    public boolean checkUserNameUnique(String userName) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserName, userName)
        );
        return count == 0;
    }

    @Override
    public boolean checkPhoneUnique(SysUser user) {
        Long userId = user.getUserId() == null ? -1L : user.getUserId();
        SysUser exist = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getPhonenumber, user.getPhonenumber())
                        .last("LIMIT 1")
        );
        // 不存在或仅自身拥有该手机号，视为唯一
        return exist == null || exist.getUserId().equals(userId);
    }

    @Override
    public boolean checkEmailUnique(SysUser user) {
        Long userId = user.getUserId() == null ? -1L : user.getUserId();
        SysUser exist = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEmail, user.getEmail())
                        .last("LIMIT 1")
        );
        return exist == null || exist.getUserId().equals(userId);
    }

    @Override
    public void insertUserRole(Long userId, java.util.List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        roleMapper.insertUserRole(userId, roleIds);
    }

    @Override
    public void deleteUserRoles(Long userId) {
        roleMapper.deleteUserRolesByUserId(userId);
    }
}
