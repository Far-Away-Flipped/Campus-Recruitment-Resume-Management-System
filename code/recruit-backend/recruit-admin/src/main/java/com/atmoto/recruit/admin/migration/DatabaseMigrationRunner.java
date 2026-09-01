package com.atmoto.recruit.admin.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 数据库增量迁移入口
 * <p>应用启动时自动执行尚未应用的迁移脚本（见 {@link DatabaseMigrationService}）。
 * 执行失败抛异常 → Spring Boot 启动失败 → docker restart 重跑（依赖迁移脚本幂等收敛）。</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseMigrationRunner implements CommandLineRunner {

    private final DatabaseMigrationService migrationService;

    @Override
    public void run(String... args) {
        migrationService.migrate();
    }
}
