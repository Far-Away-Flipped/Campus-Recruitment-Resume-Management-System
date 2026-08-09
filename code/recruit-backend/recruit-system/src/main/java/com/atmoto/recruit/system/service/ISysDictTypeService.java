package com.atmoto.recruit.system.service;

import com.atmoto.recruit.system.domain.SysDictType;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * 系统字典类型 Service 接口
 *
 * @author atmoto-recruit
 */
public interface ISysDictTypeService extends IService<SysDictType> {

    List<SysDictType> selectDictTypeList(SysDictType dictType);

    SysDictType selectDictTypeById(Long dictId);

    int insertDictType(SysDictType dictType);

    int updateDictType(SysDictType dictType);

    int deleteDictTypeByIds(Long[] dictIds);
}
