package com.atmoto.recruit.biz.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 临时文件清理定时任务
 * <p>每日凌晨3点清理上传临时目录中超过24小时的文件</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupTask {

    @Value("${file.upload-root:E:/atmoto-recruit/data}")
    private String uploadRoot;

    /** 每日凌晨3点执行 */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupTempFiles() {
        File tempDir = new File(uploadRoot, "temp");
        if (!tempDir.exists() || !tempDir.isDirectory()) {
            log.debug("临时目录不存在，跳过清理: {}", tempDir.getAbsolutePath());
            return;
        }

        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        File[] files = tempDir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        int deleted = 0;
        long freedBytes = 0;
        for (File file : files) {
            try {
                BasicFileAttributes attrs = Files.readAttributes(
                    file.toPath(), BasicFileAttributes.class);
                if (attrs.lastModifiedTime().toInstant().isBefore(cutoff)) {
                    long size = file.length();
                    if (file.delete()) {
                        deleted++;
                        freedBytes += size;
                    } else {
                        log.warn("无法删除临时文件: {}", file.getAbsolutePath());
                    }
                }
            } catch (IOException e) {
                log.warn("读取文件属性失败: {}", file.getAbsolutePath(), e);
            }
        }

        if (deleted > 0) {
            log.info("临时文件清理完成：删除 {} 个文件，释放 {} 字节", deleted, freedBytes);
        }
    }
}
