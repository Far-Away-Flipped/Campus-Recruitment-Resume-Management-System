package com.atmoto.recruit.biz.admin.service.impl;

import com.atmoto.recruit.biz.admin.service.BrandConfigService;
import com.atmoto.recruit.biz.common.domain.BrandConfig;
import com.atmoto.recruit.biz.common.mapper.BrandConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 品牌配置 Service 实现
 * <p>品牌配置为 Key-Value 模式，每条配置一行记录</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrandConfigServiceImpl implements BrandConfigService {

    private final BrandConfigMapper brandConfigMapper;

    @Override
    public BrandConfig getConfig() {
        List<BrandConfig> list = brandConfigMapper.selectList(new LambdaQueryWrapper<>());
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public int saveConfig(BrandConfig config) {
        // 按 configKey 查找已有记录，存在则更新，不存在则插入
        BrandConfig existing = brandConfigMapper.selectOne(
                new LambdaQueryWrapper<BrandConfig>()
                        .eq(BrandConfig::getConfigKey, config.getConfigKey())
                        .last("LIMIT 1"));
        if (existing != null) {
            config.setId(existing.getId());
            return brandConfigMapper.updateById(config);
        } else {
            return brandConfigMapper.insert(config);
        }
    }

    @Override
    public String getConfigValue(String key) {
        BrandConfig config = brandConfigMapper.selectOne(
                new LambdaQueryWrapper<BrandConfig>()
                        .eq(BrandConfig::getConfigKey, key)
                        .last("LIMIT 1"));
        return config != null ? config.getConfigValue() : null;
    }
}
