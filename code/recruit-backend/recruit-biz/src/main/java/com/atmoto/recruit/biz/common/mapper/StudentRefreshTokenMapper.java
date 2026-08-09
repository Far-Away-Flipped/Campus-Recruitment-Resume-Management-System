package com.atmoto.recruit.biz.common.mapper;

import com.atmoto.recruit.biz.common.domain.StudentRefreshToken;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生 Refresh Token Mapper
 * <p>用于落库存储 Refresh Token，支持吊销与轮换检测</p>
 */
@Mapper
public interface StudentRefreshTokenMapper extends BaseMapper<StudentRefreshToken> {
}
