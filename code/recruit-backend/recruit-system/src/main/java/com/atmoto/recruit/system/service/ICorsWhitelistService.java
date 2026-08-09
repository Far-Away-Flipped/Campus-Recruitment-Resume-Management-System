package com.atmoto.recruit.system.service;

import com.atmoto.recruit.system.domain.CorsWhitelistRule;

import java.util.List;

/**
 * CORS 白名单规则 Service 接口
 * <p>封装规则读写、Caffeine 缓存（{@code corsWhitelistCache}）与数据库/缓存双失效时的降级兜底逻辑。
 * 详见《HR 后台「网络管理」模块设计方案 V1.0》第 3.4/3.6/3.7 节。</p>
 * <p>本接口在阶段1仅暴露 {@code DynamicCorsConfigurationSource} 运行时判定所需的读路径，以及
 * 迁移种子数据/后续管理界面共用的幂等写路径；完整的输入校验（通配符拒绝/CIDR范围拒绝/内置规则保护，
 * 详见方案 5.1 节）随 {@code SysNetworkConfigController} 一并在阶段2实现。</p>
 */
public interface ICorsWhitelistService {

    /**
     * 获取当前有效的 CORS 白名单规则（供 DynamicCorsConfigurationSource 每次请求实时匹配）
     * <p>读路径：优先查缓存 → miss 则查 DB 并回填缓存 → DB 异常或结果为空则降级返回基于
     * DEFAULT_ORIGINS 静态常量构造的规则列表（3.7节降级兜底策略）。</p>
     *
     * @return 当前生效（is_active=1）的规则列表，任何情况下都不返回 null / 抛异常
     */
    List<CorsWhitelistRule> getEffectiveRules();

    /**
     * 新增/更新白名单规则（幂等）
     * <p>先按 {@code rule_value} 唯一键查重：存在则转 update，不存在则 insert
     * （与 BrandConfigServiceImpl.saveConfig 现有模式一致，3.6节幂等性设计）。</p>
     * <p>写路径：先写 DB 成功 → 再 invalidate 缓存，不做增量更新，下次读取触发整表重载。</p>
     *
     * @param rule 规则信息
     * @return 影响行数
     */
    int addRule(CorsWhitelistRule rule);

    /**
     * 查询全量白名单规则列表（不过滤is_active，含启用和禁用的所有规则，供管理界面展示）
     *
     * @return 全量规则列表（按id升序或创建时间，具体排序不强制要求，能返回全部即可）
     */
    List<CorsWhitelistRule> list();

    /**
     * 新增规则（含完整输入校验：格式/通配符/CIDR范围/唯一性）
     * <p>与阶段1已实现的 addRule() 不同：addRule() 是内部幂等写入（无校验，供数据迁移/降级场景使用），
     * 本方法是管理界面新增操作的入口，校验不通过时抛 BizException，不做"查重转update"的幂等语义——
     * 若ruleValue已存在应直接报错拒绝，而不是静默转成更新。</p>
     *
     * @param rule 规则信息（id应为null，由数据库自增）
     * @return 影响行数
     */
    int createRule(CorsWhitelistRule rule);

    /**
     * 更新规则（含完整输入校验，同新增）
     * <p>更新时的唯一性校验需排除自身：如果ruleValue未变，不应误判为"与自己重复"。</p>
     *
     * @param rule 规则信息（id必填，标识要更新哪条）
     * @return 影响行数
     */
    int updateRule(CorsWhitelistRule rule);

    /**
     * 删除规则（is_builtin=1 的内置规则拒绝物理删除）
     *
     * @param id 规则ID
     * @return 影响行数
     */
    int deleteRule(Long id);

    /**
     * 启用/停用规则
     *
     * @param id       规则ID
     * @param isActive 目标状态："1"启用 "0"停用
     * @return 影响行数
     */
    int toggleStatus(Long id, String isActive);
}
