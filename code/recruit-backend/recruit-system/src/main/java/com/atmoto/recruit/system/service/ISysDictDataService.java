package com.atmoto.recruit.system.service;

import com.atmoto.recruit.system.domain.SysDictData;

import java.util.List;

/**
 * 系统字典数据 Service 接口
 * <p>管理字典数据的增删改查，支持按字典类型批量查询</p>
 *
 * @author atmoto-recruit
 */
public interface ISysDictDataService {

    /**
     * 分页查询字典数据
     *
     * @param dictData 查询条件
     * @return 字典数据列表
     */
    List<SysDictData> selectDictDataList(SysDictData dictData);

    /**
     * 根据字典类型查询所有字典数据
     *
     * @param dictType 字典类型编码
     * @return 字典数据列表
     */
    List<SysDictData> selectDictDataByType(String dictType);

    /**
     * 根据字典编码查询单条字典数据
     *
     * @param dictCode 字典编码
     * @return 字典数据
     */
    SysDictData selectDictDataById(Long dictCode);

    /**
     * 新增字典数据
     *
     * @param dictData 字典数据
     * @return 影响行数
     */
    int insertDictData(SysDictData dictData);

    /**
     * 修改字典数据
     *
     * @param dictData 字典数据
     * @return 影响行数
     */
    int updateDictData(SysDictData dictData);

    /**
     * 批量删除字典数据
     *
     * @param dictCodes 字典编码数组
     * @return 影响行数
     */
    int deleteDictDataByIds(Long[] dictCodes);
}
