package com.atmoto.recruit.admin.controller.system;

import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.atmoto.recruit.framework.security.context.AdminUserHolder;
import com.atmoto.recruit.system.domain.CorsWhitelistRule;
import com.atmoto.recruit.system.domain.SysNetworkConfig;
import com.atmoto.recruit.system.mapper.SysNetworkConfigMapper;
import com.atmoto.recruit.system.service.ICorsWhitelistService;
import com.atmoto.recruit.system.service.ISysRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 网络管理 Controller —— CORS 白名单 + 局域网访问开关（阶段2 管理界面）
 * <p>路径前缀 {@code /api/system/network}，全部挂在 adminFilterChain 下，不加入任何
 * permitAll 列表。CORS 白名单读写委托 {@link ICorsWhitelistService}；局域网访问开关是
 * sys_network_config 表的一条 KV 记录，逻辑简单，未单独建 Service，直接注入 Mapper 操作。
 * 详见《HR 后台「网络管理」模块设计方案 V1.0》第 5.1/5.2 节。</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/network")
public class SysNetworkConfigController {

    /** sys_network_config 局域网开关的固定 config_key（4.4节种子数据） */
    private static final String LAN_ACCESS_KEY = "lan_access_enabled";

    private final ICorsWhitelistService corsWhitelistService;
    private final SysNetworkConfigMapper sysNetworkConfigMapper;
    private final ISysRoleService sysRoleService;

    // ──────────────── CORS 白名单管理 ────────────────

    /**
     * 查询CORS白名单全量列表（两角色可读）
     */
    @GetMapping("/cors-origins")
    public AjaxResult listCorsOrigins() {
        List<CorsWhitelistRule> list = corsWhitelistService.list();
        return AjaxResult.success(list);
    }

    /**
     * 新增CORS白名单规则（仅 hr_director）
     */
    @PostMapping("/cors-origins")
    public AjaxResult addCorsOrigin(@RequestBody CorsWhitelistRule rule) {
        requireNetworkAdminRole();
        int rows = corsWhitelistService.createRule(rule);
        return rows > 0 ? AjaxResult.success("新增白名单规则成功") : AjaxResult.error("新增白名单规则失败");
    }

    /**
     * 编辑CORS白名单规则（仅 hr_director）
     */
    @PutMapping("/cors-origins/{id}")
    public AjaxResult editCorsOrigin(@PathVariable Long id, @RequestBody CorsWhitelistRule rule) {
        requireNetworkAdminRole();
        rule.setId(id);
        int rows = corsWhitelistService.updateRule(rule);
        return rows > 0 ? AjaxResult.success("修改白名单规则成功") : AjaxResult.error("修改白名单规则失败");
    }

    /**
     * 删除CORS白名单规则（仅 hr_director，is_builtin=1 拒绝）
     */
    @DeleteMapping("/cors-origins/{id}")
    public AjaxResult deleteCorsOrigin(@PathVariable Long id) {
        requireNetworkAdminRole();
        int rows = corsWhitelistService.deleteRule(id);
        return rows > 0 ? AjaxResult.success("删除白名单规则成功") : AjaxResult.error("删除白名单规则失败");
    }

    /**
     * 启停用CORS白名单规则（仅 hr_director）
     * <p>请求体：{"isActive": "1"} 或 {"isActive": "0"}</p>
     */
    @PutMapping("/cors-origins/{id}/status")
    public AjaxResult toggleCorsOriginStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireNetworkAdminRole();
        Object isActiveObj = body.get("isActive");
        String isActive = isActiveObj != null ? isActiveObj.toString() : null;
        if (!"0".equals(isActive) && !"1".equals(isActive)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "isActive仅支持\"0\"或\"1\"");
        }
        int rows = corsWhitelistService.toggleStatus(id, isActive);
        return rows > 0 ? AjaxResult.success("状态切换成功") : AjaxResult.error("状态切换失败");
    }

    // ──────────────── 局域网访问开关 ────────────────

    /**
     * 查询局域网访问开关（两角色可读）
     */
    @GetMapping("/lan-access")
    public AjaxResult getLanAccess() {
        SysNetworkConfig config = sysNetworkConfigMapper.selectOne(
                new LambdaQueryWrapper<SysNetworkConfig>()
                        .eq(SysNetworkConfig::getConfigKey, LAN_ACCESS_KEY)
                        .last("LIMIT 1"));
        boolean enabled = config != null && "true".equalsIgnoreCase(config.getConfigValue());
        return AjaxResult.success(Map.of("lanAccessEnabled", enabled));
    }

    /**
     * 更新局域网访问开关（仅 hr_director）
     * <p>请求体：{"lanAccessEnabled": true}</p>
     */
    @PutMapping("/lan-access")
    public AjaxResult updateLanAccess(@RequestBody Map<String, Object> body) {
        requireNetworkAdminRole();
        Object v = body.get("lanAccessEnabled");
        if (!(v instanceof Boolean)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "lanAccessEnabled必须为布尔值");
        }
        String newValue = String.valueOf(v);

        SysNetworkConfig existing = sysNetworkConfigMapper.selectOne(
                new LambdaQueryWrapper<SysNetworkConfig>()
                        .eq(SysNetworkConfig::getConfigKey, LAN_ACCESS_KEY)
                        .last("LIMIT 1"));
        int rows;
        if (existing != null) {
            rows = sysNetworkConfigMapper.update(null,
                    new LambdaUpdateWrapper<SysNetworkConfig>()
                            .eq(SysNetworkConfig::getConfigKey, LAN_ACCESS_KEY)
                            .set(SysNetworkConfig::getConfigValue, newValue));
        } else {
            // 种子数据阶段1已插入该记录，理论上不会走到这个分支；兜底自愈以防记录被误删
            SysNetworkConfig config = new SysNetworkConfig();
            config.setConfigKey(LAN_ACCESS_KEY);
            config.setConfigValue(newValue);
            config.setConfigType("BOOLEAN");
            config.setConfigGroup("NETWORK");
            config.setDescription("是否允许局域网/内网IP直接访问HR后台");
            rows = sysNetworkConfigMapper.insert(config);
        }
        return rows > 0 ? AjaxResult.success("局域网访问开关更新成功") : AjaxResult.error("局域网访问开关更新失败");
    }

    // ──────────────── 权限检查 ────────────────

    /**
     * 校验当前HR是否具备网络管理"总监级"权限
     * <p>过渡方案：查询 sys_user_role → sys_role.role_key 判断是否为 hr_director。
     * 【重要】不可照抄 ResumeAdminController.hasAllDataScope() 的判断逻辑——
     * 该方法用 "sys_admin".equals(sysUser.getUserType()) 判断，但实测 init-data.sql
     * 中初始化的 admin 账号 user_type='00'，不是"sys_admin"，该逻辑在实际数据下恒为false，
     * 是一个已存在的独立bug（超出本模块范围，本模块严禁复制这个错误模式）。
     * 正确做法是查角色key，详见《HR 后台「网络管理」模块设计方案 V1.0》第 5.2 节。</p>
     */
    private void requireNetworkAdminRole() {
        Long userId = AdminUserHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        List<String> roleKeys = sysRoleService.selectRoleKeysByUserId(userId);
        if (!roleKeys.contains("hr_director")) {
            throw new BizException(ErrorCode.NETWORK_ADMIN_ROLE_REQUIRED);
        }
    }
}
