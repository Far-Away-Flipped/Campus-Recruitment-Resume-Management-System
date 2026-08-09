package com.atmoto.recruit.system.service.impl;

import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.atmoto.recruit.system.domain.CorsWhitelistRule;
import com.atmoto.recruit.system.mapper.CorsWhitelistRuleMapper;
import com.atmoto.recruit.system.service.ICorsWhitelistService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CORS 白名单规则 Service 实现
 * <p>缓存策略与降级兜底见《HR 后台「网络管理」模块设计方案 V1.0》第 3.6/3.7 节；
 * 幂等写入模式参照 {@code BrandConfigServiceImpl.saveConfig}：先按 rule_value 唯一键查重，
 * 存在则转 update，不存在则 insert。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CorsWhitelistServiceImpl implements ICorsWhitelistService {

    /** corsWhitelistCache 固定缓存 key（maximumSize=1，整表作为单一条目缓存） */
    private static final String CACHE_KEY = "ALL";

    /** EXACT类型Origin格式校验：完整来源地址（scheme://host[:port]），不含路径/查询参数/结尾斜杠 */
    private static final Pattern EXACT_ORIGIN_PATTERN =
            Pattern.compile("^https?://[a-zA-Z0-9.-]+(:\\d{1,5})?$");

    /** CIDR格式校验：IPv4字面量四段 + 前缀数字，四个捕获组用于取出各IP段数值，第五组为前缀数字 */
    private static final Pattern CIDR_FORMAT_PATTERN =
            Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})/(\\d{1,2})$");

    /**
     * 数据库与缓存双失效时的最后防线（3.7节）。
     * <p>
     * 内容与 recruit-framework 模块 {@code CorsConfig.DEFAULT_ORIGINS} 保持一致。
     * 之所以在此单独维护一份而非直接引用该常量：3.2节裁决 recruit-system 不依赖
     * recruit-framework（依赖方向相反，recruit-framework 依赖 recruit-system），
     * Service 层物理上无法引用 framework 模块的类。两处内容如需变更（如上线新域名）须同步修改。
     * </p>
     */
    private static final List<String> DEFAULT_ORIGINS = List.of(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:5174",
            "https://campus.atmoto.cn"
    );

    private final CorsWhitelistRuleMapper corsWhitelistRuleMapper;

    @Qualifier("corsWhitelistCache")
    private final Cache<String, List<CorsWhitelistRule>> corsWhitelistCache;

    @Override
    public List<CorsWhitelistRule> getEffectiveRules() {
        // 1. 尝试读缓存
        List<CorsWhitelistRule> cached = corsWhitelistCache.getIfPresent(CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        // 2. miss 则查 DB
        List<CorsWhitelistRule> rules;
        try {
            rules = corsWhitelistRuleMapper.selectList(
                    new LambdaQueryWrapper<CorsWhitelistRule>()
                            .eq(CorsWhitelistRule::getIsActive, "1"));
        } catch (Exception e) {
            // DB查询抛异常 → log.error记录 → 降级返回基于 DEFAULT_ORIGINS 构造的规则列表
            log.error("查询CORS白名单规则失败，降级使用DEFAULT_ORIGINS静态列表兜底", e);
            return buildDefaultRules();
        }

        // 3. 查询成功但结果为空 → 同样降级到 DEFAULT_ORIGINS（防御首次上线表为空的情况）
        if (rules == null || rules.isEmpty()) {
            log.warn("CORS白名单规则表查询结果为空，降级使用DEFAULT_ORIGINS静态列表兜底");
            return buildDefaultRules();
        }

        // 成功则回填缓存并返回
        corsWhitelistCache.put(CACHE_KEY, rules);
        return rules;
    }

    @Override
    public int addRule(CorsWhitelistRule rule) {
        // 按 rule_value 唯一键查重，存在则转 update，不存在则 insert
        CorsWhitelistRule existing = corsWhitelistRuleMapper.selectOne(
                new LambdaQueryWrapper<CorsWhitelistRule>()
                        .eq(CorsWhitelistRule::getRuleValue, rule.getRuleValue())
                        .last("LIMIT 1"));
        int rows;
        if (existing != null) {
            rule.setId(existing.getId());
            rows = corsWhitelistRuleMapper.updateById(rule);
        } else {
            rows = corsWhitelistRuleMapper.insert(rule);
        }
        // 先写DB成功 → 再 invalidate 缓存，不做增量更新，下次读取触发整表重载（3.6节写路径策略）
        corsWhitelistCache.invalidate(CACHE_KEY);
        return rows;
    }

    @Override
    public List<CorsWhitelistRule> list() {
        return corsWhitelistRuleMapper.selectList(null);
    }

    @Override
    public int createRule(CorsWhitelistRule rule) {
        validateRule(rule, null);
        // 新增规则强制isBuiltin=0：内置规则语义只属于阶段1种子数据的5条，
        // 避免通过管理界面新增的规则被意外标记为内置规则（不管调用方传了什么，强制覆盖）
        rule.setIsBuiltin("0");
        if (rule.getIsActive() == null || rule.getIsActive().trim().isEmpty()) {
            rule.setIsActive("1");
        }
        int rows = corsWhitelistRuleMapper.insert(rule);
        // 先写DB成功 → 再 invalidate 缓存（3.6节写路径策略）
        corsWhitelistCache.invalidate(CACHE_KEY);
        return rows;
    }

    @Override
    public int updateRule(CorsWhitelistRule rule) {
        CorsWhitelistRule existing = corsWhitelistRuleMapper.selectById(rule.getId());
        if (existing == null) {
            throw new BizException(ErrorCode.NETWORK_ORIGIN_NOT_FOUND);
        }
        validateRule(rule, rule.getId());
        // 更新时不允许修改isBuiltin：强制沿用数据库原值，防止通过编辑接口把普通规则伪装成内置规则，
        // 或把内置规则的isBuiltin改成0从而绕过删除保护
        rule.setIsBuiltin(existing.getIsBuiltin());
        int rows = corsWhitelistRuleMapper.updateById(rule);
        corsWhitelistCache.invalidate(CACHE_KEY);
        return rows;
    }

    @Override
    public int deleteRule(Long id) {
        CorsWhitelistRule existing = corsWhitelistRuleMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.NETWORK_ORIGIN_NOT_FOUND);
        }
        // 内置规则拒绝物理删除，只可禁用（is_builtin=1）
        if ("1".equals(existing.getIsBuiltin())) {
            throw new BizException(ErrorCode.NETWORK_BUILTIN_RULE_PROTECTED);
        }
        int rows = corsWhitelistRuleMapper.deleteById(id);
        corsWhitelistCache.invalidate(CACHE_KEY);
        return rows;
    }

    @Override
    public int toggleStatus(Long id, String isActive) {
        CorsWhitelistRule existing = corsWhitelistRuleMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.NETWORK_ORIGIN_NOT_FOUND);
        }
        // 内置规则允许被禁用（方案明确"内置规则拒绝物理删除，只可禁用"），此处无需对isBuiltin做拦截
        LambdaUpdateWrapper<CorsWhitelistRule> updateWrapper = new LambdaUpdateWrapper<CorsWhitelistRule>()
                .eq(CorsWhitelistRule::getId, id)
                .set(CorsWhitelistRule::getIsActive, isActive);
        int rows = corsWhitelistRuleMapper.update(null, updateWrapper);
        corsWhitelistCache.invalidate(CACHE_KEY);
        return rows;
    }

    /**
     * 规则输入校验（新增/更新共用）
     * <p>校验顺序固定：非空/长度 → ruleType枚举 → EXACT/CIDR专项格式与范围校验 → 唯一性校验，
     * 顺序不可打乱——尤其EXACT分支中"通配符判断"必须先于"正则格式校验"，否则含*的输入会先被
     * 正则分支拦截成格式错误（NETWORK_ORIGIN_FORMAT_INVALID），导致 NETWORK_ORIGIN_WILDCARD_FORBIDDEN
     * 这个错误码永远不会被触发。详见《HR 后台「网络管理」模块设计方案 V1.0》第 5.1 节。</p>
     *
     * @param rule      待校验规则
     * @param excludeId 唯一性校验时排除的记录id（新增传null，更新传当前记录id，避免"未改值却误判重复"）
     */
    private void validateRule(CorsWhitelistRule rule, Long excludeId) {
        // 1. ruleValue 非空 + 长度校验
        String ruleValue = rule.getRuleValue();
        if (ruleValue == null || ruleValue.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "ruleValue不能为空");
        }
        if (ruleValue.length() > 128) {
            throw new BizException(ErrorCode.PARAM_INVALID, "ruleValue长度不能超过128字符");
        }

        // 2. ruleType 枚举校验
        String ruleType = rule.getRuleType();
        if (!"EXACT".equals(ruleType) && !"CIDR".equals(ruleType)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "ruleType仅支持EXACT或CIDR");
        }

        if ("EXACT".equals(ruleType)) {
            // 3a. 通配符判断必须在正则校验之前：正则本身也会拒绝含*的字符串，
            // 若只写正则校验，含通配符的输入会被正则分支拦截成格式错误，永远走不到这里
            if (ruleValue.contains("*")) {
                throw new BizException(ErrorCode.NETWORK_ORIGIN_WILDCARD_FORBIDDEN);
            }
            // 3b. 完整Origin格式校验（scheme://host[:port]，不含路径/查询参数/结尾斜杠）
            if (!EXACT_ORIGIN_PATTERN.matcher(ruleValue).matches()) {
                throw new BizException(ErrorCode.NETWORK_ORIGIN_FORMAT_INVALID);
            }
        } else {
            // 4a. CIDR 格式校验：先用正则拆出IPv4字面量与前缀数字
            Matcher matcher = CIDR_FORMAT_PATTERN.matcher(ruleValue);
            if (!matcher.matches()) {
                throw new BizException(ErrorCode.NETWORK_CIDR_FORMAT_INVALID);
            }
            int[] octets = new int[4];
            for (int i = 0; i < 4; i++) {
                octets[i] = Integer.parseInt(matcher.group(i + 1));
                // IP每段数值超出0~255同样算格式非法，避免"999.999.999.999/99"这种输入漏网
                if (octets[i] < 0 || octets[i] > 255) {
                    throw new BizException(ErrorCode.NETWORK_CIDR_FORMAT_INVALID);
                }
            }
            int prefixLength = Integer.parseInt(matcher.group(5));
            if (prefixLength < 0 || prefixLength > 32) {
                throw new BizException(ErrorCode.NETWORK_CIDR_FORMAT_INVALID);
            }

            // 4b. 前缀长度校验：前缀数字越小网段范围越大（/8是1600万地址的大网段，/24是256地址的小网段），
            // "拒绝短于/16的提交"即"拒绝前缀数字比16更小的提交"
            if (prefixLength < 16) {
                throw new BizException(ErrorCode.NETWORK_CIDR_RANGE_FORBIDDEN);
            }

            // 4c. RFC1918私有地址范围校验：网络地址部分须落在 10.0.0.0/8、172.16.0.0/12、192.168.0.0/16 三者之一
            // 手工用long承载32位IP做位运算，避免符号位问题，且不引入spring-security的IpAddressMatcher
            // （recruit-system模块未依赖spring-boot-starter-security，见pom.xml）
            long submittedIp = ipToLong(octets[0], octets[1], octets[2], octets[3]);
            boolean inPrivateRange =
                    isSubnetOf(submittedIp, ipToLong(10, 0, 0, 0), 8)
                            || isSubnetOf(submittedIp, ipToLong(172, 16, 0, 0), 12)
                            || isSubnetOf(submittedIp, ipToLong(192, 168, 0, 0), 16);
            if (!inPrivateRange) {
                throw new BizException(ErrorCode.NETWORK_CIDR_RANGE_FORBIDDEN);
            }
        }

        // 5. 唯一性校验（大小写不敏感），更新场景排除自身id，否则更新一条记录但不改ruleValue时会误判为重复
        LambdaQueryWrapper<CorsWhitelistRule> uniqueWrapper = new LambdaQueryWrapper<CorsWhitelistRule>()
                .apply("LOWER(rule_value) = LOWER({0})", ruleValue);
        if (excludeId != null) {
            uniqueWrapper.ne(CorsWhitelistRule::getId, excludeId);
        }
        CorsWhitelistRule existingRule = corsWhitelistRuleMapper.selectOne(uniqueWrapper.last("LIMIT 1"));
        if (existingRule != null) {
            throw new BizException("EXACT".equals(ruleType)
                    ? ErrorCode.NETWORK_ORIGIN_DUPLICATE
                    : ErrorCode.NETWORK_CIDR_DUPLICATE);
        }
    }

    /**
     * 判断 subnetIp 所代表的网段是否落在 parentIp/parentPrefix 父网段内
     * <p>用 (ip & mask) 比较网络地址部分是否相等来判断子网关系。</p>
     */
    private boolean isSubnetOf(long subnetIp, long parentIp, int parentPrefix) {
        long mask = maskFor(parentPrefix);
        return (subnetIp & mask) == (parentIp & mask);
    }

    /** 根据前缀长度计算32位子网掩码（如 /8 → 0xFF000000），全程用long承载，避免int位运算的符号位问题 */
    private long maskFor(int prefix) {
        if (prefix == 0) {
            return 0L;
        }
        return (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
    }

    /** 将IPv4四段（0~255）拼接为32位整数，用long承载避免最高位被当作负数 */
    private long ipToLong(int a, int b, int c, int d) {
        return ((long) a << 24) | ((long) b << 16) | ((long) c << 8) | d;
    }

    /**
     * 基于 DEFAULT_ORIGINS 静态常量构造降级规则列表
     * <p>不回填缓存——避免在 DB 恢复后仍长期复用降级状态（corsWhitelistCache 无TTL，
     * 只能靠写操作主动 invalidate，若把降级结果写入缓存会导致 DB 恢复后无法自愈）。</p>
     */
    private List<CorsWhitelistRule> buildDefaultRules() {
        List<CorsWhitelistRule> defaults = new ArrayList<>(DEFAULT_ORIGINS.size());
        LocalDateTime now = LocalDateTime.now();
        for (String origin : DEFAULT_ORIGINS) {
            CorsWhitelistRule rule = new CorsWhitelistRule();
            rule.setRuleType("EXACT");
            rule.setRuleValue(origin);
            rule.setDescription("降级兜底规则（数据库/缓存双失效，来自DEFAULT_ORIGINS静态列表）");
            rule.setIsActive("1");
            rule.setIsBuiltin("1");
            rule.setCreateTime(now);
            defaults.add(rule);
        }
        return defaults;
    }
}
