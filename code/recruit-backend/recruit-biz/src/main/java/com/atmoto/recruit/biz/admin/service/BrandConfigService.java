package com.atmoto.recruit.biz.admin.service;

import com.atmoto.recruit.biz.common.domain.BrandConfig;

/**
 * 品牌配置 Service 接口
 * <p>管理系统品牌信息（公司名称、品牌色、Logo等），单记录配置</p>
 *
 * @author atmoto-recruit
 */
public interface BrandConfigService {

    /**
     * 获取品牌配置（单记录）
     *
     * @return 品牌配置，不存在则返回 null
     */
    BrandConfig getConfig();

    /**
     * 保存或更新品牌配置
     * <p>首次调用插入，后续调用更新</p>
     *
     * @param config 品牌配置
     * @return 影响行数
     */
    int saveConfig(BrandConfig config);

    /**
     * 按配置键查询单个配置值
     *
     * @param key 配置键
     * @return 配置值，不存在返回 null
     */
    String getConfigValue(String key);
}
