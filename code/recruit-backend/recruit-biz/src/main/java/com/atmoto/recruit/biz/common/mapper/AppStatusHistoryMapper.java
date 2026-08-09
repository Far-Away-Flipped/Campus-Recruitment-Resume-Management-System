package com.atmoto.recruit.biz.common.mapper;

import com.atmoto.recruit.biz.common.domain.AppStatusHistory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 状态流转历史 Mapper
 *
 * @author atmoto-recruit
 */
@Mapper
public interface AppStatusHistoryMapper extends BaseMapper<AppStatusHistory> {
}
