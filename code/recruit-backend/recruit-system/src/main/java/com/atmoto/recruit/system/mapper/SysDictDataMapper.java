package com.atmoto.recruit.system.mapper;

import com.atmoto.recruit.system.domain.SysDictData;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictData> {

    /** 根据字典类型查所有字典数据 */
    // 注意：BaseEntity.delFlag 带 @TableLogic，但手写 SQL 不会自动追加逻辑删除条件，
    //       必须显式加 del_flag = '0'，否则软删字典项会泄漏到门户/后台下拉（历史 bug 已修）
    @Select("SELECT * FROM sys_dict_data WHERE dict_type = #{dictType} AND status = '0' AND del_flag = '0' ORDER BY dict_sort")
    List<SysDictData> selectByDictType(String dictType);
}
