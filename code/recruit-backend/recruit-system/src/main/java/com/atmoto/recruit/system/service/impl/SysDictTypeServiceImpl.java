package com.atmoto.recruit.system.service.impl;

import com.atmoto.recruit.system.domain.SysDictType;
import com.atmoto.recruit.system.mapper.SysDictTypeMapper;
import com.atmoto.recruit.system.service.ISysDictTypeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 系统字典类型 Service 实现
 *
 * @author atmoto-recruit
 */
@Service
@RequiredArgsConstructor
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType> implements ISysDictTypeService {

    private final SysDictTypeMapper dictTypeMapper;

    @Override
    public List<SysDictType> selectDictTypeList(SysDictType dictType) {
        LambdaQueryWrapper<SysDictType> qw = new LambdaQueryWrapper<>();
        if (dictType.getDictName() != null) qw.like(SysDictType::getDictName, dictType.getDictName());
        if (dictType.getDictType() != null) qw.eq(SysDictType::getDictType, dictType.getDictType());
        if (dictType.getStatus() != null) qw.eq(SysDictType::getStatus, dictType.getStatus());
        qw.orderByAsc(SysDictType::getDictId);
        return list(qw);
    }

    @Override
    public SysDictType selectDictTypeById(Long dictId) {
        return getById(dictId);
    }

    @Override
    public int insertDictType(SysDictType dictType) {
        return save(dictType) ? 1 : 0;
    }

    @Override
    public int updateDictType(SysDictType dictType) {
        return updateById(dictType) ? 1 : 0;
    }

    @Override
    public int deleteDictTypeByIds(Long[] dictIds) {
        return removeByIds(Arrays.asList(dictIds)) ? 1 : 0;
    }
}
