package com.atmoto.recruit.system.mapper;

import com.atmoto.recruit.system.domain.SysMenu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /** 根据用户ID查询其拥有的菜单权限 */
    @Select("SELECT DISTINCT m.* FROM sys_menu m " +
            "LEFT JOIN sys_role_menu rm ON m.menu_id = rm.menu_id " +
            "LEFT JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.status = '0' " +
            "ORDER BY m.parent_id, m.order_num")
    List<SysMenu> selectMenusByUserId(Long userId);
}
