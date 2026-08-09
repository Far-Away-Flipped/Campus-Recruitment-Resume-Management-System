package com.atmoto.recruit.biz.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 数据库定时备份任务
 * <p>每日凌晨1点调用外部 PowerShell 备份脚本，执行 mysqldump + 7-Zip压缩加密 + robocopy附件增量同步。
 * 备份脚本位于 recruit-admin/src/main/resources/scripts/lib/backup.ps1</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@Component
public class DatabaseBackupTask {

    /** 备份脚本路径，可通过配置覆盖，默认指向 classpath 外部署路径 */
    @Value("${backup.script-path:E:/atmoto-recruit/scripts/lib/backup.ps1}")
    private String backupScriptPath;

    /**
     * 每日凌晨1点触发数据库备份
     * <p>开发环境可配置 cron 表达式或通过 spring.profiles.active=dev 跳过</p>
     */
    @Scheduled(cron = "${backup.cron:0 0 1 * * ?}")
    public void triggerBackup() {
        log.info("数据库备份任务触发，脚本路径：{}", backupScriptPath);

        try {
            // 调用外部 PowerShell 脚本执行备份
            // 使用 -ExecutionPolicy Bypass 确保脚本可执行（生产环境建议用签名脚本）
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell",
                    "-ExecutionPolicy", "Bypass",
                    "-File", backupScriptPath);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("数据库备份任务完成：exitCode={}, output={}", exitCode, output);
            } else {
                log.error("数据库备份任务失败：exitCode={}, output={}", exitCode, output);
            }
        } catch (Exception e) {
            log.error("数据库备份任务执行异常", e);
            // 开发环境仅记录日志，不中断应用
        }
    }
}
