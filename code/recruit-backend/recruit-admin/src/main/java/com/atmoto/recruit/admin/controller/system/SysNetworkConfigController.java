package com.atmoto.recruit.admin.controller.system;

import com.atmoto.recruit.admin.controller.system.vo.NetworkDiagnosticsVO;
import com.atmoto.recruit.biz.common.security.AdminRoleGuard;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.common.core.domain.TableDataInfo;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.atmoto.recruit.common.utils.IpUtils;
import com.atmoto.recruit.framework.security.context.AdminUserHolder;
import com.atmoto.recruit.system.domain.CorsWhitelistRule;
import com.atmoto.recruit.system.domain.NetworkAuditContext;
import com.atmoto.recruit.system.domain.NetworkConfigLog;
import com.atmoto.recruit.system.domain.SysNetworkConfig;
import com.atmoto.recruit.system.mapper.NetworkConfigLogMapper;
import com.atmoto.recruit.system.mapper.SysNetworkConfigMapper;
import com.atmoto.recruit.system.service.ICorsWhitelistService;
import com.atmoto.recruit.system.service.ISysRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网络管理 Controller —— CORS 白名单 + 局域网访问开关 + 网络诊断 + 变更审计（阶段2+阶段3）
 * <p>路径前缀 {@code /api/system/network}，全部挂在 adminFilterChain 下，不加入任何
 * permitAll 列表。CORS 白名单读写委托 {@link ICorsWhitelistService}；局域网访问开关是
 * sys_network_config 表的一条 KV 记录，逻辑简单，未单独建 Service，直接注入 Mapper 操作。
 * 诊断接口(5.1节"网络诊断")与变更审计接口(4.2.3/5.1节)详见《HR 后台「网络管理」模块设计方案
 * V1.0》。</p>
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
    private final NetworkConfigLogMapper networkConfigLogMapper;
    private final ISysRoleService sysRoleService;
    private final AdminRoleGuard adminRoleGuard;
    private final ServerProperties serverProperties;
    private final HttpServletRequest request;

    // ──────────────── CORS 白名单管理 ────────────────

    /**
     * 查询CORS白名单全量列表（两角色可读）
     */
    @GetMapping("/cors-origins")
    public AjaxResult listCorsOrigins() {
        adminRoleGuard.requireDirector();
        List<CorsWhitelistRule> list = corsWhitelistService.list();
        return AjaxResult.success(list);
    }

    /**
     * 新增CORS白名单规则（仅超级管理员 admin）
     */
    @PostMapping("/cors-origins")
    public AjaxResult addCorsOrigin(@RequestBody CorsWhitelistRule rule) {
        requireNetworkAdminRole();
        int rows = corsWhitelistService.createRule(rule, currentAuditContext());
        return rows > 0 ? AjaxResult.success("新增白名单规则成功") : AjaxResult.error("新增白名单规则失败");
    }

    /**
     * 编辑CORS白名单规则（仅超级管理员 admin）
     */
    @PutMapping("/cors-origins/{id}")
    public AjaxResult editCorsOrigin(@PathVariable Long id, @RequestBody CorsWhitelistRule rule) {
        requireNetworkAdminRole();
        rule.setId(id);
        int rows = corsWhitelistService.updateRule(rule, currentAuditContext());
        return rows > 0 ? AjaxResult.success("修改白名单规则成功") : AjaxResult.error("修改白名单规则失败");
    }

    /**
     * 删除CORS白名单规则（仅超级管理员 admin，is_builtin=1 拒绝）
     */
    @DeleteMapping("/cors-origins/{id}")
    public AjaxResult deleteCorsOrigin(@PathVariable Long id) {
        requireNetworkAdminRole();
        int rows = corsWhitelistService.deleteRule(id, currentAuditContext());
        return rows > 0 ? AjaxResult.success("删除白名单规则成功") : AjaxResult.error("删除白名单规则失败");
    }

    /**
     * 启停用CORS白名单规则（仅超级管理员 admin）
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
        int rows = corsWhitelistService.toggleStatus(id, isActive, currentAuditContext());
        return rows > 0 ? AjaxResult.success("状态切换成功") : AjaxResult.error("状态切换失败");
    }

    // ──────────────── 局域网访问开关 ────────────────

    /**
     * 查询局域网访问开关（两角色可读）
     */
    @GetMapping("/lan-access")
    public AjaxResult getLanAccess() {
        adminRoleGuard.requireDirector();
        SysNetworkConfig config = sysNetworkConfigMapper.selectOne(
                new LambdaQueryWrapper<SysNetworkConfig>()
                        .eq(SysNetworkConfig::getConfigKey, LAN_ACCESS_KEY)
                        .last("LIMIT 1"));
        boolean enabled = config != null && "true".equalsIgnoreCase(config.getConfigValue());
        return AjaxResult.success(Map.of("lanAccessEnabled", enabled));
    }

    /**
     * 更新局域网访问开关（仅超级管理员 admin）
     * <p>请求体：{"lanAccessEnabled": true}</p>
     * <p>写入成功后落一条 audit_network_config 审计记录（config_table=NETWORK_CONFIG，
     * operation_type=TOGGLE），详见方案 4.2.3 节。</p>
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
        String oldValue = existing != null ? existing.getConfigValue() : null;
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
        if (rows > 0) {
            writeNetworkConfigAudit(null, "NETWORK_CONFIG", "TOGGLE", oldValue, newValue,
                    "切换局域网访问开关");
        }
        return rows > 0 ? AjaxResult.success("局域网访问开关更新成功") : AjaxResult.error("局域网访问开关更新失败");
    }

    // ──────────────── 网络诊断（只读） ────────────────

    /**
     * 网络诊断（两角色可读）
     * <p>
     * 【安全边界·强制】响应严格走白名单式 {@link NetworkDiagnosticsVO} 组装，绝不直接序列化
     * {@code Environment}/{@code ServerProperties} 整个对象，不夹带 jwt.admin.secret、数据库
     * 连接串等其他配置项。诊断内容是"被动推断当前请求是否经过预期的Nginx转发"，不做任何反向
     * 探测/出站请求。详见《HR 后台「网络管理」模块设计方案 V1.0》第 5.1 节。
     * </p>
     */
    @GetMapping("/diagnostics")
    public AjaxResult diagnostics() {
        adminRoleGuard.requireDirector();
        String forwardedFor = blankToNull(request.getHeader("X-Forwarded-For"));
        String forwardedProto = blankToNull(request.getHeader("X-Forwarded-Proto"));

        List<String> corsOrigins = corsWhitelistService.getEffectiveRules().stream()
                .map(CorsWhitelistRule::getRuleValue)
                .collect(Collectors.toList());

        SysNetworkConfig lanConfig = sysNetworkConfigMapper.selectOne(
                new LambdaQueryWrapper<SysNetworkConfig>()
                        .eq(SysNetworkConfig::getConfigKey, LAN_ACCESS_KEY)
                        .last("LIMIT 1"));
        boolean lanAccessEnabled = lanConfig != null && "true".equalsIgnoreCase(lanConfig.getConfigValue());

        NetworkDiagnosticsVO vo = new NetworkDiagnosticsVO(
                serverProperties.getAddress() != null ? serverProperties.getAddress().getHostAddress() : null,
                serverProperties.getPort(),
                corsOrigins,
                lanAccessEnabled,
                IpUtils.getClientIp(request),
                request.getHeader("Host"),
                forwardedFor,
                forwardedProto,
                forwardedFor != null || forwardedProto != null
        );
        return AjaxResult.success(vo);
    }

    // ──────────────── 变更审计历史 ────────────────

    /**
     * 分页查询网络配置变更审计历史（两角色可读）
     * <p>支持按 configType(=config_table)/operationType/operatorName 筛选，风格参照
     * {@code AuditLogController.list()}。详见方案 5.1 节。</p>
     */
    @GetMapping("/audit/list")
    public AjaxResult auditList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String configType,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String operatorName) {

        adminRoleGuard.requireDirector();
        LambdaQueryWrapper<NetworkConfigLog> wrapper = new LambdaQueryWrapper<>();
        if (configType != null && !configType.isEmpty()) {
            wrapper.eq(NetworkConfigLog::getConfigTable, configType);
        }
        if (operationType != null && !operationType.isEmpty()) {
            wrapper.eq(NetworkConfigLog::getOperationType, operationType);
        }
        if (operatorName != null && !operatorName.isEmpty()) {
            wrapper.like(NetworkConfigLog::getOperatorName, operatorName);
        }
        wrapper.orderByDesc(NetworkConfigLog::getCreateTime);

        Page<NetworkConfigLog> page = new Page<>(pageNum, pageSize);
        Page<NetworkConfigLog> result = networkConfigLogMapper.selectPage(page, wrapper);

        return AjaxResult.page(TableDataInfo.of((int) result.getTotal(), result.getRecords()));
    }

    // ──────────────── 权限检查 ────────────────

    /**
     * 校验当前HR是否具备网络管理"超级管理员"权限
     * <p>过渡方案：查询 sys_user_role → sys_role.role_key 判断是否为 admin。
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
        if (!roleKeys.contains("admin")) {
            throw new BizException(ErrorCode.NETWORK_ADMIN_ROLE_REQUIRED);
        }
    }

    /**
     * 组装当前请求的审计上下文（操作人 + 来源IP + User-Agent），传给 ICorsWhitelistService
     * 写方法用于落审计记录。
     * <p>之所以由 Controller 构造而非 Service 直接读取：{@link AdminUserHolder} 定义在
     * recruit-framework 模块，recruit-system 反向依赖 framework 会构成环形依赖（详见方案
     * 3.2节裁决1）。Controller 所在的 recruit-admin 模块同时依赖两者，天然适合做这层转换。</p>
     */
    private NetworkAuditContext currentAuditContext() {
        return new NetworkAuditContext(
                AdminUserHolder.getUserId(),
                AdminUserHolder.getUsername(),
                IpUtils.getClientIp(request),
                request.getHeader("User-Agent")
        );
    }

    /**
     * 局域网开关变更的审计埋点（复用 CorsWhitelistServiceImpl 相同的字段语义，但局域网开关
     * 不经过 ICorsWhitelistService，故 Controller 直接调用 Mapper 落审计记录）。
     */
    private void writeNetworkConfigAudit(Long ruleId, String configTable, String operationType,
                                          String oldValue, String newValue, String operationDetail) {
        try {
            NetworkAuditContext context = currentAuditContext();
            NetworkConfigLog logEntry = new NetworkConfigLog();
            logEntry.setRuleId(ruleId);
            logEntry.setConfigTable(configTable);
            logEntry.setOperationType(operationType);
            logEntry.setOldValue(oldValue);
            logEntry.setNewValue(newValue);
            logEntry.setOperationDetail(operationDetail);
            logEntry.setOperatorId(context.getOperatorId());
            logEntry.setOperatorName(context.getOperatorName() != null ? context.getOperatorName() : "system");
            logEntry.setIpAddress(context.getIpAddress());
            logEntry.setUserAgent(context.getUserAgent());
            logEntry.setCreateTime(java.time.LocalDateTime.now());
            networkConfigLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.error("写入网络配置变更审计记录失败：configTable={}, operationType={}", configTable, operationType, e);
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
