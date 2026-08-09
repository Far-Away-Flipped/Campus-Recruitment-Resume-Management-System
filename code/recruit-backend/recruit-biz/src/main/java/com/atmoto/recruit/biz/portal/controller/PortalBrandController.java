package com.atmoto.recruit.biz.portal.controller;

import com.atmoto.recruit.biz.admin.service.BrandConfigService;
import com.atmoto.recruit.biz.common.domain.BrandConfig;
import com.atmoto.recruit.biz.common.domain.SysBanner;
import com.atmoto.recruit.biz.common.mapper.BrandConfigMapper;
import com.atmoto.recruit.biz.common.mapper.SysBannerMapper;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 门户端品牌配置 Controller（公开访问）
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portal/brand")
public class PortalBrandController {

    private final BrandConfigService brandConfigService;
    private final BrandConfigMapper brandConfigMapper;
    private final SysBannerMapper sysBannerMapper;

    @GetMapping
    public AjaxResult getPublicBrandInfo() {
        // Return ALL config entries as key-value map (no XSS exposure)
        List<BrandConfig> allConfigs = brandConfigMapper.selectList(new LambdaQueryWrapper<>());
        Map<String, String> result = new HashMap<>();
        for (BrandConfig cfg : allConfigs) {
            if (cfg.getConfigValue() != null) {
                result.put(cfg.getConfigKey(), HtmlUtils.htmlEscape(cfg.getConfigValue()));
            }
        }
        return AjaxResult.success(result);
    }

    /** 公开 Banner 列表（门户首页调用） */
    @GetMapping("/banners")
    public AjaxResult getPublicBanners() {
        LambdaQueryWrapper<SysBanner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysBanner::getStatus, "1")
               .eq(SysBanner::getDelFlag, "0")
               .orderByAsc(SysBanner::getSortOrder);
        List<SysBanner> banners = sysBannerMapper.selectList(wrapper);
        return AjaxResult.success(banners);
    }
}
