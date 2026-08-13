package com.atmoto.recruit.biz.admin.controller;

import com.atmoto.recruit.biz.admin.service.BrandConfigService;
import com.atmoto.recruit.biz.common.domain.BrandConfig;
import com.atmoto.recruit.biz.common.security.AdminRoleGuard;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

/**
 * 品牌配置 Controller（A-001）
 * <p>管理系统品牌信息：公司名称、品牌色、Logo 等</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/brand")
public class BrandConfigController {

    private final BrandConfigService brandConfigService;
    private final AdminRoleGuard adminRoleGuard;

    /**
     * 获取品牌配置
     */
    @GetMapping("/config")
    public AjaxResult getConfig() {
        adminRoleGuard.requireDirector();
        BrandConfig config = brandConfigService.getConfig();
        return AjaxResult.success(config);
    }

    /**
     * 保存或更新品牌配置
     */
    @PostMapping("/config")
    public AjaxResult saveConfig(@RequestBody BrandConfig config) {
        adminRoleGuard.requireDirector();
        if (config.getConfigValue() != null) {
            config.setConfigValue(HtmlUtils.htmlEscape(config.getConfigValue()));
        }
        if (config.getDescription() != null) {
            config.setDescription(HtmlUtils.htmlEscape(config.getDescription()));
        }
        int rows = brandConfigService.saveConfig(config);
        return rows > 0 ? AjaxResult.success("品牌配置保存成功") : AjaxResult.error("品牌配置保存失败");
    }
}
