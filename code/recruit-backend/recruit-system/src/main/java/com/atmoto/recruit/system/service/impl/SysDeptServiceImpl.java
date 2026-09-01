package com.atmoto.recruit.system.service.impl;

import com.atmoto.recruit.system.domain.SysDept;
import com.atmoto.recruit.system.mapper.SysDeptMapper;
import com.atmoto.recruit.system.service.ISysDeptService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统部门 Service 实现
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl implements ISysDeptService {

    private final SysDeptMapper deptMapper;

    @Override
    public List<SysDept> selectDeptList(SysDept dept) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        if (dept.getDeptName() != null && !dept.getDeptName().isEmpty()) {
            wrapper.like(SysDept::getDeptName, dept.getDeptName());
        }
        if (dept.getStatus() != null && !dept.getStatus().isEmpty()) {
            wrapper.eq(SysDept::getStatus, dept.getStatus());
        }
        wrapper.orderByAsc(SysDept::getParentId, SysDept::getOrderNum);
        return deptMapper.selectList(wrapper);
    }

    @Override
    public SysDept selectDeptById(Long deptId) {
        return deptMapper.selectById(deptId);
    }

    @Override
    public int insertDept(SysDept dept) {
        // 计算 ancestors 字段
        if (dept.getParentId() != null && dept.getParentId() != 0) {
            SysDept parent = deptMapper.selectById(dept.getParentId());
            if (parent != null) {
                dept.setAncestors(parent.getAncestors() + "," + dept.getParentId());
            }
        } else {
            dept.setAncestors("0");
        }
        return deptMapper.insert(dept);
    }

    @Override
    public int updateDept(SysDept dept) {
        // 更新时重算 ancestors
        if (dept.getParentId() != null && dept.getParentId() != 0) {
            SysDept parent = deptMapper.selectById(dept.getParentId());
            if (parent != null) {
                dept.setAncestors(parent.getAncestors() + "," + dept.getParentId());
            }
        } else {
            dept.setAncestors("0");
        }
        return deptMapper.updateById(dept);
    }

    @Override
    public int deleteDeptById(Long deptId) {
        return deptMapper.deleteById(deptId);
    }

    @Override
    public boolean checkDeptNameUnique(SysDept dept) {
        Long deptId = dept.getDeptId() == null ? -1L : dept.getDeptId();
        SysDept exist = deptMapper.selectOne(
                new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getDeptName, dept.getDeptName())
                        .eq(SysDept::getParentId, dept.getParentId())
                        .last("LIMIT 1")
        );
        return exist == null || exist.getDeptId().equals(deptId);
    }

    @Override
    public boolean hasChildByDeptId(Long deptId) {
        // 检查是否存在以 deptId 为祖先的活跃部门（含直接子节点与更深层后代）。
        // 不能只查 parent_id = deptId：若直接子节点已被逻辑删除（del_flag=2）但孙级后代仍活跃，
        // 只查直接子节点会漏判，删除该部门后孙级后代将成为孤儿（parent 指向已删部门）。
        // ancestors 格式为 "0,父id,祖id..."，用 CONCAT(',',ancestors,',') 包裹后做精确子串匹配，
        // 避免 deptId 与祖先数字串的误匹配（如 deptId=1 不会误命中 "0,11"）。
        Long count = deptMapper.selectCount(
                new LambdaQueryWrapper<SysDept>()
                        .apply("CONCAT(',', ancestors, ',') LIKE CONCAT('%,', {0}, ',%')", deptId)
        );
        return count > 0;
    }
}
