package com.atmoto.recruit.admin.controller.system;

import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.common.core.domain.TableDataInfo;
import com.atmoto.recruit.common.core.page.PageQuery;
import com.atmoto.recruit.system.domain.SysRole;
import com.atmoto.recruit.system.service.ISysRoleService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统角色 Controller —— 角色 CRUD
 * <p>管理 HR 角色及其关联权限</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/role")
public class SysRoleController {

    private final ISysRoleService roleService;

    /**
     * 查询角色列表（MyBatis-Plus 分页）
     */
    @GetMapping("/list")
    public AjaxResult list(SysRole role, PageQuery pageQuery) {
        IPage<SysRole> page = roleService.selectRolePage(role, pageQuery);
        return AjaxResult.page(TableDataInfo.of(page.getTotal(), page.getRecords()));
    }

    /**
     * 查询所有正常状态的角色（用于下拉选）
     */
    @GetMapping("/all")
    public AjaxResult all() {
        List<SysRole> list = roleService.selectRoleAll();
        return AjaxResult.success(list);
    }

    /**
     * 查询角色详情
     */
    @GetMapping("/{roleId}")
    public AjaxResult getInfo(@PathVariable Long roleId) {
        SysRole role = roleService.selectRoleById(roleId);
        return AjaxResult.success(role);
    }

    /**
     * 新增角色
     */
    @PostMapping
    public AjaxResult add(@RequestBody SysRole role) {
        if (!roleService.checkRoleNameUnique(role)) {
            return AjaxResult.error("角色名称已存在");
        }
        if (!roleService.checkRoleKeyUnique(role)) {
            return AjaxResult.error("角色权限字符串已存在");
        }
        int rows = roleService.insertRole(role);
        return rows > 0 ? AjaxResult.success("新增角色成功") : AjaxResult.error("新增角色失败");
    }

    /**
     * 修改角色
     */
    @PutMapping
    public AjaxResult edit(@RequestBody SysRole role) {
        if (!roleService.checkRoleNameUnique(role)) {
            return AjaxResult.error("角色名称已存在");
        }
        if (!roleService.checkRoleKeyUnique(role)) {
            return AjaxResult.error("角色权限字符串已存在");
        }
        int rows = roleService.updateRole(role);
        return rows > 0 ? AjaxResult.success("修改角色成功") : AjaxResult.error("修改角色失败");
    }

    /**
     * 删除角色（批量）
     */
    @DeleteMapping("/{roleIds}")
    public AjaxResult remove(@PathVariable Long[] roleIds) {
        int rows = roleService.deleteRoleByIds(roleIds);
        return rows > 0 ? AjaxResult.success("删除角色成功") : AjaxResult.error("删除角色失败");
    }
}
