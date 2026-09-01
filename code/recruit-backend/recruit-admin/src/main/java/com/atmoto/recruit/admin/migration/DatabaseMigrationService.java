package com.atmoto.recruit.admin.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据库增量迁移服务
 * <p>约定：initdb 基线 = V0，迁移从 V1 起。版本文件命名 {@code V{n}__描述.sql}，
 * 放在 classpath 的 {@code db/migration/} 目录。已应用版本记录在 {@code schema_version} 表，
 * 每次启动只执行新增版本；迁移文件须幂等（崩溃重跑不报错）。</p>
 *
 * <p>执行细节：每个文件用 {@link ScriptUtils} 逐条执行（MySQL DDL 隐式提交，不配事务），
 * 整个文件成功后才写入版本行；失败抛异常使启动失败（fail fast），docker restart 后重跑。</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseMigrationService {

    /** 版本文件枚举模式（classpath* 在解包目录与 fat jar 嵌套 classpath 下均可列出） */
    private static final String MIGRATION_LOCATION = "classpath*:db/migration/*.sql";
    private static final Pattern VERSION_FILE_PATTERN = Pattern.compile("^V(\\d+)__.*\\.sql$");

    private static final String CREATE_VERSION_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS `schema_version` (
                `version`     INT          NOT NULL COMMENT '迁移版本号，与 V{n} 前缀对应',
                `description` VARCHAR(255) DEFAULT NULL COMMENT '迁移描述（文件名 __ 后部分）',
                `checksum`    CHAR(32)     DEFAULT NULL COMMENT 'SQL 文件内容 MD5（hex）',
                `applied_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '应用时间',
                PRIMARY KEY (`version`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库增量迁移版本表（V0=initdb基线，迁移从V1起）'
            """;

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    /** 已应用版本文件被篡改时的行为：warn 仅告警（默认），fail 阻断启动 */
    @Value("${app.migration.checksum-mismatch:warn}")
    private String checksumMismatchMode;

    /** 执行全部未应用的迁移 */
    public void migrate() {
        log.info("数据库迁移：开始");
        createVersionTable();

        Set<Integer> appliedVersions = loadAppliedVersions();
        Map<Integer, Resource> pending = discoverPending(appliedVersions);

        if (pending.isEmpty()) {
            log.info("数据库迁移：无需应用新版本，已应用 {} 个版本 {}", appliedVersions.size(), appliedVersions);
            return;
        }

        List<Integer> appliedNow = new ArrayList<>();
        for (Map.Entry<Integer, Resource> entry : pending.entrySet()) {
            int version = entry.getKey();
            Resource resource = entry.getValue();
            String description = resource.getFilename();
            String checksum = checksumOf(resource);

            log.info("数据库迁移：应用版本 V{} - {}", version, description);
            try {
                executeScript(resource);
                insertVersionRecord(version, description, checksum);
                appliedNow.add(version);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "数据库迁移 V" + version + "（" + description + "）执行失败，已应用的 DDL 无法回滚，" +
                        "修复脚本后重启将重新执行（迁移文件须幂等）。原始错误: " + e.getMessage(), e);
            }
        }
        log.info("数据库迁移：完成，本次应用 {} 个版本 {}，累计已应用 {} 个版本",
                appliedNow.size(), appliedNow, appliedVersions.size() + appliedNow.size());
    }

    /** 建版本表（幂等） */
    private void createVersionTable() {
        jdbcTemplate.execute(CREATE_VERSION_TABLE_SQL);
    }

    /** 读取已应用版本号集合 */
    private Set<Integer> loadAppliedVersions() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT version, checksum FROM schema_version");
        Set<Integer> versions = new HashSet<>();
        for (Map<String, Object> row : rows) {
            Object v = row.get("version");
            if (v != null) versions.add(((Number) v).intValue());
        }
        return versions;
    }

    /**
     * 发现待应用迁移：枚举全部版本文件 → 过滤掉已应用 → 校验 checksum → 按版本升序
     * <p>枚举用 {@code classpath*:db/migration/*.sql}：Spring 的 PathMatchingResourcePatternResolver
     * 对 fat jar 内 BOOT-INF/classes 下的平铺 classpath 资源可正常枚举（与加载 mapper XML 同机制）。</p>
     */
    private Map<Integer, Resource> discoverPending(Set<Integer> applied) {
        Map<Integer, Resource> all = new TreeMap<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources(MIGRATION_LOCATION);
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;
                Matcher m = VERSION_FILE_PATTERN.matcher(filename);
                if (!m.matches()) {
                    log.warn("数据库迁移：忽略非版本文件 {}", filename);
                    continue;
                }
                int version = Integer.parseInt(m.group(1));
                if (all.containsKey(version)) {
                    throw new IllegalStateException("数据库迁移：发现重复版本文件 V" + version
                            + "（" + all.get(version).getFilename() + " 与 " + filename + "），请修正命名");
                }
                all.put(version, resource);
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("数据库迁移：枚举迁移文件失败（" + MIGRATION_LOCATION + "）: " + e.getMessage(), e);
        }

        // checksum 校验：已应用版本，重算文件 MD5 对比
        for (Integer version : all.keySet()) {
            if (!applied.contains(version)) continue;
            Resource resource = all.get(version);
            String stored = storedChecksum(version);
            String current = checksumOf(resource);
            if (stored != null && current != null && !stored.equals(current)) {
                String msg = "数据库迁移：已应用版本 V" + version + " 的文件内容与记录不一致（checksum 不匹配）。"
                        + "已应用版本禁止编辑，如需变更请新增版本文件。";
                if ("fail".equalsIgnoreCase(checksumMismatchMode)) {
                    throw new IllegalStateException(msg);
                }
                log.warn("数据库迁移：{}（当前 {} vs 记录 {}）", msg, current, stored);
            }
        }

        Map<Integer, Resource> pending = new HashMap<>(all);
        pending.keySet().removeAll(applied);
        return new TreeMap<>(pending);
    }

    /** 单文件逐条执行（ScriptUtils 状态机拆 SQL，正确处理字符串/注释/中文里的分号） */
    private void executeScript(Resource resource) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true); // DDL 隐式提交，显式声明逐条自动提交
            ScriptUtils.executeSqlScript(connection,
                    new EncodedResource(resource, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("数据库迁移：执行脚本 " + resource.getFilename() + " 失败: " + e.getMessage(), e);
        }
    }

    /** 版本成功应用后写入版本记录 */
    private void insertVersionRecord(int version, String description, String checksum) {
        jdbcTemplate.update("INSERT INTO schema_version (version, description, checksum) VALUES (?, ?, ?)",
                version, description, checksum);
    }

    /** 查询某版本已记录的 checksum */
    private String storedChecksum(int version) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT checksum FROM schema_version WHERE version = ?", version);
        return rows.isEmpty() ? null : (String) rows.get(0).get("checksum");
    }

    /** 计算迁移文件内容 MD5（hex） */
    private String checksumOf(Resource resource) {
        try {
            return DigestUtils.md5DigestAsHex(resource.getInputStream());
        } catch (Exception e) {
            return null;
        }
    }
}
