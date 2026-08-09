package com.atmoto.recruit.biz.admin.service;

import com.atmoto.recruit.biz.common.domain.SysBanner;

import java.util.List;

/**
 * Banner 公告 Service 接口
 * <p>管理首页 Banner 轮播图的增删改查</p>
 *
 * @author atmoto-recruit
 */
public interface SysBannerService {

    /**
     * 分页查询 Banner 列表
     *
     * @param banner 查询条件
     * @return Banner 列表
     */
    List<SysBanner> selectBannerList(SysBanner banner);

    /**
     * 根据ID查询 Banner
     *
     * @param id Banner ID
     * @return Banner 信息
     */
    SysBanner selectBannerById(Long id);

    /**
     * 新增 Banner
     *
     * @param banner Banner 信息
     * @return 影响行数
     */
    int insertBanner(SysBanner banner);

    /**
     * 修改 Banner
     *
     * @param banner Banner 信息
     * @return 影响行数
     */
    int updateBanner(SysBanner banner);

    /**
     * 批量删除 Banner
     *
     * @param ids ID 数组
     * @return 影响行数
     */
    int deleteBannerByIds(Long[] ids);
}
