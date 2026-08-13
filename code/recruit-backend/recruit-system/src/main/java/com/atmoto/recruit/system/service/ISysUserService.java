package com.atmoto.recruit.system.service;

import com.atmoto.recruit.common.core.page.PageQuery;
import com.atmoto.recruit.system.domain.SysUser;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 系统用户 Service 接口
 * <p>管理 HR 和管理员账号的增删改查、密码重置、状态变更等操作</p>
 *
 * @author atmoto-recruit
 */
public interface ISysUserService {

    /**
     * 根据用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    SysUser selectUserById(Long userId);

    /**
     * 根据用户名查询用户（用于登录认证）
     *
     * @param userName 用户名
     * @return 用户信息
     */
    SysUser selectUserByUserName(String userName);

    /**
     * 分页查询用户列表
     *
     * @param user 查询条件
     * @return 用户列表
     */
    List<SysUser> selectUserList(SysUser user);

    /**
     * 分页查询用户列表（MyBatis-Plus 分页）
     *
     * @param user      查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    IPage<SysUser> selectUserPage(SysUser user, PageQuery pageQuery);

    /**
     * 新增用户
     *
     * @param user 用户信息
     * @return 影响行数
     */
    int insertUser(SysUser user);

    /**
     * 修改用户信息
     *
     * @param user 用户信息
     * @return 影响行数
     */
    int updateUser(SysUser user);

    /**
     * 批量删除用户（逻辑删除）
     *
     * @param userIds 用户ID数组
     * @return 影响行数
     */
    int deleteUserByIds(Long[] userIds);

    /**
     * 重置用户密码
     *
     * @param user 包含 userId 和新密码的对象
     * @return 影响行数
     */
    int resetPwd(SysUser user);

    /**
     * 修改用户状态（停用/启用）
     *
     * @param user 包含 userId 和 status 的对象
     * @return 影响行数
     */
    int updateUserStatus(SysUser user);

    /**
     * 校验用户名是否唯一
     *
     * @param userName 用户名
     * @return true=唯一
     */
    boolean checkUserNameUnique(String userName);

    /**
     * 校验手机号码是否唯一
     *
     * @param user 包含 userId（编辑时排除自身）和 phonenumber
     * @return true=唯一
     */
    boolean checkPhoneUnique(SysUser user);

    /**
     * 校验邮箱是否唯一
     *
     * @param user 包含 userId 和 email
     * @return true=唯一
     */
    boolean checkEmailUnique(SysUser user);

    /**
     * 给用户批量绑定角色（幂等，ON DUPLICATE KEY）
     *
     * @param userId  用户ID
     * @param roleIds 角色ID列表
     */
    void insertUserRole(Long userId, java.util.List<Long> roleIds);

    /**
     * 删除用户的全部角色绑定（角色同步前的清理）
     *
     * @param userId 用户ID
     */
    void deleteUserRoles(Long userId);
}
