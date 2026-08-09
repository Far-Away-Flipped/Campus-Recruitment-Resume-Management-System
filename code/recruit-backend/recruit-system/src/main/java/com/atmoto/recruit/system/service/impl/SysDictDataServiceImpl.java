package com.atmoto.recruit.system.service.impl;

import com.atmoto.recruit.system.domain.SysDictData;
import com.atmoto.recruit.system.mapper.SysDictDataMapper;
import com.atmoto.recruit.system.service.ISysDictDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 系统字典数据 Service 实现
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictDataServiceImpl implements ISysDictDataService {

    private final SysDictDataMapper dictDataMapper;

    @Override
    public List<SysDictData> selectDictDataList(SysDictData dictData) {
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        if (dictData.getDictType() != null && !dictData.getDictType().isEmpty()) {
            wrapper.eq(SysDictData::getDictType, dictData.getDictType());
        }
        if (dictData.getDictLabel() != null && !dictData.getDictLabel().isEmpty()) {
            wrapper.like(SysDictData::getDictLabel, dictData.getDictLabel());
        }
        if (dictData.getStatus() != null && !dictData.getStatus().isEmpty()) {
            wrapper.eq(SysDictData::getStatus, dictData.getStatus());
        }
        wrapper.orderByAsc(SysDictData::getDictSort);
        return dictDataMapper.selectList(wrapper);
    }

    @Override
    public List<SysDictData> selectDictDataByType(String dictType) {
        return dictDataMapper.selectByDictType(dictType);
    }

    @Override
    public SysDictData selectDictDataById(Long dictCode) {
        return dictDataMapper.selectById(dictCode);
    }

    @Override
    public int insertDictData(SysDictData dictData) {
        return dictDataMapper.insert(dictData);
    }

    @Override
    public int updateDictData(SysDictData dictData) {
        return dictDataMapper.updateById(dictData);
    }

    @Override
    public int deleteDictDataByIds(Long[] dictCodes) {
        return dictDataMapper.deleteBatchIds(Arrays.asList(dictCodes));
    }
}
