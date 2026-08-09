package com.atmoto.recruit.biz.admin.controller;

import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.system.domain.SysDept;
import com.atmoto.recruit.system.service.ISysDeptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门树 Controller —— 前端兼容路径
 * <p>前端 HR 后台调用 /api/admin/depts/tree 获取部门树，
 * 本 Controller 桥接到 recruit-system 的 ISysDeptService 并组装带 children 的树结构</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/depts")
public class DeptTreeController {

    private final ISysDeptService deptService;

    /**
     * 查询部门树（含 children 嵌套）
     * <p>前端 el-tree 等树形组件直接消费此结构</p>
     */
    @GetMapping("/tree")
    public AjaxResult tree() {
        List<SysDept> all = deptService.selectDeptList(new SysDept());
        List<Map<String, Object>> tree = buildDeptTree(all, null);
        return AjaxResult.success(tree);
    }

    // ────────────────── 内部工具方法 ──────────────────

    /**
     * 递归构建部门树
     *
     * @param all      全部部门列表（平铺）
     * @param parentId 当前层级父节点ID（null 表示根节点）
     * @return 树形部门列表
     */
    private List<Map<String, Object>> buildDeptTree(List<SysDept> all, Long parentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysDept dept : all) {
            // 匹配父节点：parentId 为 null 时匹配 parentId 为 0 或 null 的根节点
            Long deptParentId = dept.getParentId();
            boolean isRoot = (deptParentId == null || deptParentId == 0L);
            boolean matchesParent = (parentId == null && isRoot)
                    || (parentId != null && parentId.equals(deptParentId));

            if (matchesParent) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("deptId", dept.getDeptId());
                node.put("parentId", deptParentId);
                node.put("deptName", dept.getDeptName());
                node.put("orderNum", dept.getOrderNum());
                node.put("leader", dept.getLeader());
                node.put("phone", dept.getPhone());
                node.put("email", dept.getEmail());
                node.put("status", dept.getStatus());
                node.put("createTime", dept.getCreateTime());
                node.put("updateTime", dept.getUpdateTime());

                List<Map<String, Object>> children = buildDeptTree(all, dept.getDeptId());
                node.put("children", children);

                result.add(node);
            }
        }
        return result;
    }
}
