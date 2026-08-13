package com.atmoto.recruit.admin.controller.system;

import com.atmoto.recruit.biz.common.security.AdminRoleGuard;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.system.domain.SysDept;
import com.atmoto.recruit.system.service.ISysDeptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统部门 Controller —— 部门 CRUD
 * <p>管理部门树形结构</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/dept")
public class SysDeptController {

    private final ISysDeptService deptService;
    private final AdminRoleGuard adminRoleGuard;

    /**
     * 查询部门列表
     */
    @GetMapping("/list")
    public AjaxResult list(SysDept dept) {
        adminRoleGuard.requireDirector();
        List<SysDept> list = deptService.selectDeptList(dept);
        return AjaxResult.success(list);
    }

    /**
     * 查询部门详情
     */
    @GetMapping("/{deptId}")
    public AjaxResult getInfo(@PathVariable Long deptId) {
        adminRoleGuard.requireDirector();
        SysDept dept = deptService.selectDeptById(deptId);
        return AjaxResult.success(dept);
    }

    /**
     * 新增部门
     */
    @PostMapping
    public AjaxResult add(@RequestBody SysDept dept) {
        adminRoleGuard.requireDirector();
        // 校验部门名称唯一性
        if (!deptService.checkDeptNameUnique(dept)) {
            return AjaxResult.error("部门名称已存在");
        }
        int rows = deptService.insertDept(dept);
        return rows > 0 ? AjaxResult.success("新增部门成功") : AjaxResult.error("新增部门失败");
    }

    /**
     * 修改部门
     */
    @PutMapping
    public AjaxResult edit(@RequestBody SysDept dept) {
        adminRoleGuard.requireDirector();
        // 校验部门名称唯一性
        if (!deptService.checkDeptNameUnique(dept)) {
            return AjaxResult.error("部门名称已存在");
        }
        int rows = deptService.updateDept(dept);
        return rows > 0 ? AjaxResult.success("修改部门成功") : AjaxResult.error("修改部门失败");
    }

    /**
     * 删除部门
     * <p>存在子部门时不允许删除</p>
     */
    @DeleteMapping("/{deptId}")
    public AjaxResult remove(@PathVariable Long deptId) {
        adminRoleGuard.requireDirector();
        if (deptService.hasChildByDeptId(deptId)) {
            return AjaxResult.error("该部门下存在子部门，无法删除");
        }
        int rows = deptService.deleteDeptById(deptId);
        return rows > 0 ? AjaxResult.success("删除部门成功") : AjaxResult.error("删除部门失败");
    }
}
