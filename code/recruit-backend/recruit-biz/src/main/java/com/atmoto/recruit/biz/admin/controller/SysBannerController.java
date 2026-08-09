package com.atmoto.recruit.biz.admin.controller;

import com.atmoto.recruit.biz.admin.service.SysBannerService;
import com.atmoto.recruit.biz.common.domain.SysBanner;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Banner 公告 Controller（A-001）
 * <p>管理首页轮播图 Banner 的增删改查</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/banner")
public class SysBannerController {

    private final SysBannerService bannerService;

    /**
     * 查询 Banner 列表
     */
    @GetMapping("/list")
    public AjaxResult list(SysBanner banner) {
        List<SysBanner> list = bannerService.selectBannerList(banner);
        return AjaxResult.success(list);
    }

    /**
     * 查询 Banner 详情
     */
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        SysBanner banner = bannerService.selectBannerById(id);
        return AjaxResult.success(banner);
    }

    /**
     * 新增 Banner
     */
    @PostMapping
    public AjaxResult add(@RequestBody SysBanner banner) {
        int rows = bannerService.insertBanner(banner);
        return rows > 0 ? AjaxResult.success("创建成功", Map.of("id", banner.getId())) : AjaxResult.error("新增Banner失败");
    }

    /**
     * 修改 Banner
     */
    @PutMapping
    public AjaxResult edit(@RequestBody SysBanner banner) {
        int rows = bannerService.updateBanner(banner);
        return rows > 0 ? AjaxResult.success("修改Banner成功") : AjaxResult.error("修改Banner失败");
    }

    /**
     * 删除 Banner
     */
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        int rows = bannerService.deleteBannerByIds(ids);
        return rows > 0 ? AjaxResult.success("删除Banner成功") : AjaxResult.error("删除Banner失败");
    }
}
