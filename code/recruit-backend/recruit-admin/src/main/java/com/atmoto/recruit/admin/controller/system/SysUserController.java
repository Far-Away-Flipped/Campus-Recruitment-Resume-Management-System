package com.atmoto.recruit.admin.controller.system;

import com.atmoto.recruit.biz.common.security.AdminRoleGuard;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.common.core.domain.TableDataInfo;
import com.atmoto.recruit.common.core.page.PageQuery;
import com.atmoto.recruit.system.domain.SysUser;
import com.atmoto.recruit.system.service.ISysRoleService;
import com.atmoto.recruit.system.service.ISysUserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统用户 Controller —— HR 账号 CRUD（A-003）
 * <p>基于 RuoYi sys_user 表管理 HR 和管理员账号</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/user")
public class SysUserController {

    private final ISysUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AdminRoleGuard adminRoleGuard;
    private final ISysRoleService roleService;

    /** 受保护的超级管理员账号（不可删除/禁用/移除 admin 角色） */
    private static final String PROTECTED_ADMIN = "AT-admin";

    /**
     * 分页查询用户列表（MyBatis-Plus 分页）
     */
    @GetMapping("/list")
    public AjaxResult list(SysUser user, PageQuery pageQuery) {
        adminRoleGuard.requireDirector();
        IPage<SysUser> page = userService.selectUserPage(user, pageQuery);
        List<SysUser> records = page.getRecords();
        if (records != null && !records.isEmpty()) {
            List<Long> userIds = records.stream().map(SysUser::getUserId).collect(Collectors.toList());
            List<Map<String, Object>> roleRows = roleService.selectRolesByUserIds(userIds);
            Map<Long, List<Long>> roleIdsMap = new HashMap<>();
            Map<Long, List<String>> roleNamesMap = new HashMap<>();
            for (Map<String, Object> row : roleRows) {
                Long uid = row.get("user_id") != null ? ((Number) row.get("user_id")).longValue() : null;
                Long roleId = row.get("role_id") != null ? ((Number) row.get("role_id")).longValue() : null;
                String roleName = row.get("role_name") != null ? row.get("role_name").toString() : null;
                if (uid == null) continue;
                if (roleId != null) roleIdsMap.computeIfAbsent(uid, k -> new ArrayList<>()).add(roleId);
                if (roleName != null) roleNamesMap.computeIfAbsent(uid, k -> new ArrayList<>()).add(roleName);
            }
            for (SysUser u : records) {
                u.setRoleIds(roleIdsMap.getOrDefault(u.getUserId(), new ArrayList<>()));
                u.setRoleNames(roleNamesMap.getOrDefault(u.getUserId(), new ArrayList<>()));
            }
        }
        return AjaxResult.page(TableDataInfo.of(page.getTotal(), page.getRecords()));
    }

    /**
     * 查询用户详情
     */
    @GetMapping("/{userId}")
    public AjaxResult getInfo(@PathVariable Long userId) {
        adminRoleGuard.requireDirector();
        SysUser user = userService.selectUserById(userId);
        // 密码不返回前端
        if (user != null) {
            user.setPassword(null);
        }
        return AjaxResult.success(user);
    }

    /**
     * 新增用户
     * <p>手动解析请求体，字段白名单过滤，仅取实体字段，忽略 roleIds 等非实体字段</p>
     */
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> body) {
        adminRoleGuard.requireDirector();
        // 字段白名单解析：只取 SysUser 实体属性
        String userName = str(body, "userName");
        String password = str(body, "password");
        String nickName = str(body, "nickName");
        Long deptId = lng(body, "deptId");
        String phonenumber = str(body, "phonenumber");
        String email = str(body, "email");
        String status = str(body, "status");
        List<Long> roleIds = roleIdList(body);

        // 必填校验
        if (userName == null || userName.isBlank()) return AjaxResult.error("用户名不能为空");
        if (password == null || password.isEmpty()) return AjaxResult.error("密码不能为空");
        // 密码强度校验：至少8位，含字母和数字
        if (password.length() < 8 || !password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*")) {
            return AjaxResult.error("密码强度不足：至少8位且需包含字母和数字");
        }

        // 校验用户名唯一性
        if (!userService.checkUserNameUnique(userName)) {
            return AjaxResult.error("用户名已存在");
        }

        // 构建 SysUser 实体
        SysUser user = new SysUser();
        user.setUserName(userName);
        user.setNickName(nickName != null ? nickName : userName);
        user.setDeptId(deptId);
        user.setPhonenumber(phonenumber);
        user.setEmail(email);
        // 默认值：userType="00", sex="0", status="0"
        user.setUserType("00");
        user.setSex("0");
        user.setStatus(status != null && !status.isBlank() ? status : "0");

        // 校验手机号和邮箱唯一性
        if (user.getPhonenumber() != null && !userService.checkPhoneUnique(user)) {
            return AjaxResult.error("手机号码已存在");
        }
        if (user.getEmail() != null && !userService.checkEmailUnique(user)) {
            return AjaxResult.error("邮箱已存在");
        }
        // BCrypt 加密密码
        if (password != null && !password.isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        int rows = userService.insertUser(user);
        if (rows > 0) {
            // 角色持久化：未传角色则默认绑定 hr 角色
            List<Long> bindRoleIds = roleIds;
            if (bindRoleIds == null || bindRoleIds.isEmpty()) {
                Long hrRoleId = roleService.selectRoleIdByRoleKey("hr");
                bindRoleIds = hrRoleId != null ? new ArrayList<>(List.of(hrRoleId)) : new ArrayList<>();
            }
            userService.insertUserRole(user.getUserId(), bindRoleIds);
            return AjaxResult.success("新增用户成功");
        }
        return AjaxResult.error("新增用户失败");
    }

    /**
     * 修改用户信息
     */
    @PutMapping
    public AjaxResult edit(@RequestBody SysUser user) {
        adminRoleGuard.requireDirector();
        // 校验手机号和邮箱唯一性（编辑时排除自身）
        if (user.getPhonenumber() != null && !userService.checkPhoneUnique(user)) {
            return AjaxResult.error("手机号码已存在");
        }
        if (user.getEmail() != null && !userService.checkEmailUnique(user)) {
            return AjaxResult.error("邮箱已存在");
        }
        // AT-admin 保护：禁止修改用户名/用户类型，防止破坏超管身份
        SysUser existing = user.getUserId() != null ? userService.selectUserById(user.getUserId()) : null;
        boolean protectedAdmin = existing != null && PROTECTED_ADMIN.equals(existing.getUserName());
        if (protectedAdmin) {
            user.setUserName(existing.getUserName());
            user.setUserType(existing.getUserType());
        }
        // 编辑时不修改密码（密码通过 /resetPwd 独立修改）
        user.setPassword(null);
        int rows = userService.updateUser(user);
        if (rows > 0) {
            // 角色同步：仅当本次请求显式携带 roleIds 时执行（先删后插）
            if (user.getRoleIds() != null) {
                List<Long> bindRoleIds = user.getRoleIds();
                if (protectedAdmin) {
                    // AT-admin 强制保留 admin 角色
                    Long adminRoleId = roleService.selectRoleIdByRoleKey("admin");
                    if (adminRoleId != null && !bindRoleIds.contains(adminRoleId)) {
                        bindRoleIds.add(adminRoleId);
                    }
                }
                userService.deleteUserRoles(user.getUserId());
                userService.insertUserRole(user.getUserId(), bindRoleIds);
            }
            return AjaxResult.success("修改用户成功");
        }
        return AjaxResult.error("修改用户失败");
    }

    /**
     * 重置密码
     */
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody Map<String, Object> body) {
        adminRoleGuard.requireDirector();
        Long userId = body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : null;
        String password = body.get("password") != null ? body.get("password").toString() : null;
        if (userId == null) return AjaxResult.error("用户ID不能为空");
        if (password == null || password.isEmpty()) return AjaxResult.error("密码不能为空");
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setPassword(passwordEncoder.encode(password));
        int rows = userService.resetPwd(user);
        return rows > 0 ? AjaxResult.success("密码重置成功") : AjaxResult.error("密码重置失败");
    }

    /**
     * 修改用户状态（停用/启用）
     */
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysUser user) {
        adminRoleGuard.requireDirector();
        // AT-admin 保护：禁止禁用超级管理员账号
        SysUser existing = user.getUserId() != null ? userService.selectUserById(user.getUserId()) : null;
        if (existing != null && PROTECTED_ADMIN.equals(existing.getUserName()) && "1".equals(user.getStatus())) {
            return AjaxResult.error("超级管理员账号不可禁用");
        }
        int rows = userService.updateUserStatus(user);
        return rows > 0 ? AjaxResult.success("状态修改成功") : AjaxResult.error("状态修改失败");
    }

    /**
     * 删除用户（批量）
     */
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds) {
        adminRoleGuard.requireDirector();
        // AT-admin 保护：禁止删除超级管理员账号
        for (Long id : userIds) {
            SysUser u = userService.selectUserById(id);
            if (u != null && PROTECTED_ADMIN.equals(u.getUserName())) {
                return AjaxResult.error("超级管理员账号不可删除");
            }
        }
        int rows = userService.deleteUserByIds(userIds);
        return rows > 0 ? AjaxResult.success("删除用户成功") : AjaxResult.error("删除用户失败");
    }

    // ──────── 辅助方法 ────────
    private static String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null && !v.toString().isBlank() ? v.toString() : null;
    }
    private static Long lng(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private static List<Long> roleIdList(Map<String, Object> map) {
        Object v = map.get("roleIds");
        if (!(v instanceof List)) {
            return null;
        }
        List<Long> result = new ArrayList<>();
        for (Object item : (List<Object>) v) {
            if (item instanceof Number) {
                result.add(((Number) item).longValue());
            } else if (item != null) {
                try { result.add(Long.parseLong(item.toString())); } catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }
}
