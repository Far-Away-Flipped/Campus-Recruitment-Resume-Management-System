package com.atmoto.recruit.system.util;

import com.atmoto.recruit.system.domain.SysDept;
import com.atmoto.recruit.system.mapper.SysDeptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门树工具
 * <p>提供"部门自身 + 全部后代 ID"收集能力，供各筛选场景复用：
 * 选择父部门时，检索父部门及其全部子部门的岗位/用户。</p>
 *
 * @author atmoto-recruit
 */
public final class DeptTreeUtil {

    private DeptTreeUtil() {
    }

    /**
     * 收集目标部门自身及其全部后代部门的 ID
     * <p>sys_dept 的 ancestors 存祖先链（如 "0,1"），用 CONCAT 包裹做精确子串匹配，
     * 避免 deptId 与祖先数字串误匹配（如 1 不误命中 "0,11"）。</p>
     *
     * @param deptId      目标部门 ID
     * @param deptMapper  部门 Mapper（注入调用方的 SysDeptMapper）
     * @param onlyEnabled 是否只收集启用状态（status='0'）的部门
     */
    public static List<Long> collectDeptAndDescendants(Long deptId, SysDeptMapper deptMapper, boolean onlyEnabled) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        if (onlyEnabled) {
            wrapper.eq(SysDept::getStatus, "0");
        }
        List<SysDept> all = deptMapper.selectList(wrapper);
        List<Long> result = new ArrayList<>();
        for (SysDept dept : all) {
            if (dept.getDeptId().equals(deptId)) {
                result.add(dept.getDeptId());
                continue;
            }
            String ancestors = dept.getAncestors();
            if (ancestors != null && ("," + ancestors + ",").contains("," + deptId + ",")) {
                result.add(dept.getDeptId());
            }
        }
        return result;
    }
}
