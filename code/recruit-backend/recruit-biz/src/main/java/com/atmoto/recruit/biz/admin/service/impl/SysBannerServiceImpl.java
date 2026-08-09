package com.atmoto.recruit.biz.admin.service.impl;

import com.atmoto.recruit.biz.admin.service.SysBannerService;
import com.atmoto.recruit.biz.common.domain.SysBanner;
import com.atmoto.recruit.biz.common.mapper.SysBannerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Banner 公告 Service 实现
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysBannerServiceImpl implements SysBannerService {

    private final SysBannerMapper bannerMapper;

    @Override
    public List<SysBanner> selectBannerList(SysBanner banner) {
        LambdaQueryWrapper<SysBanner> wrapper = new LambdaQueryWrapper<>();
        if (banner.getTitle() != null && !banner.getTitle().isEmpty()) {
            wrapper.like(SysBanner::getTitle, banner.getTitle());
        }
        if (banner.getStatus() != null && !banner.getStatus().isEmpty()) {
            wrapper.eq(SysBanner::getStatus, banner.getStatus());
        }
        wrapper.orderByAsc(SysBanner::getSortOrder);
        return bannerMapper.selectList(wrapper);
    }

    @Override
    public SysBanner selectBannerById(Long id) {
        return bannerMapper.selectById(id);
    }

    @Override
    public int insertBanner(SysBanner banner) {
        return bannerMapper.insert(banner);
    }

    @Override
    public int updateBanner(SysBanner banner) {
        return bannerMapper.updateById(banner);
    }

    @Override
    public int deleteBannerByIds(Long[] ids) {
        return bannerMapper.deleteBatchIds(Arrays.asList(ids));
    }
}
