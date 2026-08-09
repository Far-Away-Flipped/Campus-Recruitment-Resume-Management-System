package com.atmoto.recruit.system.mapper;

import com.atmoto.recruit.system.domain.SysDictData;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictData> {

    /** 根据字典类型查所有字典数据 */
    @Select("SELECT * FROM sys_dict_data WHERE dict_type = #{dictType} AND status = '0' ORDER BY dict_sort")
    List<SysDictData> selectByDictType(String dictType);
}
