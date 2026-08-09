package com.atmoto.recruit.system.service;

import com.atmoto.recruit.system.domain.SysDept;

import java.util.List;

/**
 * 系统部门 Service 接口
 * <p>管理部门树结构，支持增删改查和树形数据构建</p>
 *
 * @author atmoto-recruit
 */
public interface ISysDeptService {

    /**
     * 查询部门树列表
     *
     * @param dept 查询条件
     * @return 部门列表（未组装树结构，由前端自行构建）
     */
    List<SysDept> selectDeptList(SysDept dept);

    /**
     * 根据部门ID查询部门详情
     *
     * @param deptId 部门ID
     * @return 部门信息
     */
    SysDept selectDeptById(Long deptId);

    /**
     * 新增部门
     *
     * @param dept 部门信息
     * @return 影响行数
     */
    int insertDept(SysDept dept);

    /**
     * 修改部门信息
     *
     * @param dept 部门信息
     * @return 影响行数
     */
    int updateDept(SysDept dept);

    /**
     * 删除部门（存在子部门时不可删除）
     *
     * @param deptId 部门ID
     * @return 影响行数
     */
    int deleteDeptById(Long deptId);

    /**
     * 校验部门名称是否唯一
     *
     * @param dept 部门信息（含 deptId 用于编辑时排除自身）
     * @return true=唯一
     */
    boolean checkDeptNameUnique(SysDept dept);

    /**
     * 检查该部门是否存在子部门
     *
     * @param deptId 部门ID
     * @return true=存在子部门
     */
    boolean hasChildByDeptId(Long deptId);
}
