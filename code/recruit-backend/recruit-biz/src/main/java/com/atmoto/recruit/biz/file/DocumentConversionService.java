package com.atmoto.recruit.biz.file;

import com.atmoto.recruit.biz.admin.service.BrandConfigService;
import com.atmoto.recruit.biz.common.domain.ResumeFile;
import com.atmoto.recruit.biz.common.mapper.ResumeFileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 文档转换服务 - Word(DOCX/DOC) to PDF
 *
 * LibreOffice path lookup (priority high to low):
 *   1. Brand config page "libreoffice_path" field (stored in sys_brand_config, editable in Admin UI)
 *   2. application.yml libreoffice.path setting
 *   3. Auto-detect common install paths (Program Files / Program Files (x86))
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentConversionService {

    private final ResumeFileMapper resumeFileMapper;
    private final BrandConfigService brandConfigService;

    @Value("${file.upload-root:E:/atmoto-recruit/data}")
    private String uploadRoot;

    @Value("${libreoffice.path:}")
    private String configuredLibreOfficePath;

    private volatile String resolvedLibreOfficePath;
    private volatile boolean libreOfficeAvailable = false;

    private static final String[] AUTO_DETECT_PATHS = {
        "D:/Program Files/LibreOffice/program/soffice.exe",
        "C:/Program Files/LibreOffice/program/soffice.exe",
        "C:/Program Files (x86)/LibreOffice/program/soffice.exe",
        "soffice"
    };

    @PostConstruct
    public void init() {
        log.warn("DocumentConversionService initializing...");
        resolveLibreOfficePath();
        detectLibreOffice();
        log.warn("DocumentConversionService init complete: available={} path={}", libreOfficeAvailable, resolvedLibreOfficePath);
    }

    /** Resolve LibreOffice path from all sources */
    private void resolveLibreOfficePath() {
        // 1. Brand config (DB, editable by admin)
        try {
            String dbPath = brandConfigService.getConfigValue("libreoffice_path");
            if (dbPath != null && !dbPath.isBlank()) {
                resolvedLibreOfficePath = dbPath.trim();
                log.info("LibreOffice path from brand config: {}", resolvedLibreOfficePath);
                return;
            }
        } catch (Exception e) {
            log.debug("BrandConfig not available for libreoffice_path lookup: {}", e.getMessage());
        }

        // 2. application.yml
        if (configuredLibreOfficePath != null && !configuredLibreOfficePath.isBlank()) {
            resolvedLibreOfficePath = configuredLibreOfficePath.trim();
            log.info("LibreOffice path from application.yml: {}", resolvedLibreOfficePath);
            return;
        }

        // 3. Auto-detect
        for (String path : AUTO_DETECT_PATHS) {
            if (Files.exists(Paths.get(path))) {
                resolvedLibreOfficePath = path;
                log.info("LibreOffice auto-detected at: {}", path);
                return;
            }
        }

        resolvedLibreOfficePath = "soffice";
        log.warn("LibreOffice not found. Install it and configure path in Admin > Brand Config.");
    }

    private void detectLibreOffice() {
        // 1. Check if file exists (fast, no hang risk)
        File exe = new File(resolvedLibreOfficePath);
        if (!exe.exists()) {
            log.warn("LibreOffice not found at: {}", resolvedLibreOfficePath);
            libreOfficeAvailable = false;
            return;
        }

        // 2. Quick functional test: try converting a tiny test doc
        try {
            Path testDir = Paths.get(uploadRoot, "temp");
            Files.createDirectories(testDir);
            Path testDoc = testDir.resolve("_lo_test_.txt");
            Files.writeString(testDoc, "test");

            ProcessBuilder pb = new ProcessBuilder(
                resolvedLibreOfficePath,
                "--headless", "--norestore", "--nologo",
                "--convert-to", "pdf",
                "--outdir", testDir.toAbsolutePath().toString(),
                testDoc.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean finished = p.waitFor(15, TimeUnit.SECONDS);

            // Cleanup
            Files.deleteIfExists(testDoc);
            Files.deleteIfExists(testDir.resolve("_lo_test_.pdf"));

            if (finished && p.exitValue() == 0) {
                libreOfficeAvailable = true;
                log.info("LibreOffice ready: {}", resolvedLibreOfficePath);
            } else {
                if (!finished) p.destroyForcibly();
                log.warn("LibreOffice functional test failed at: {}", resolvedLibreOfficePath);
            }
        } catch (Exception e) {
            log.warn("LibreOffice detection failed ({}): {}", resolvedLibreOfficePath, e.getMessage());
        }
    }

    /** Re-resolve path (called after admin updates brand config) */
    public void refreshPath() {
        resolveLibreOfficePath();
        detectLibreOffice();
    }

    public boolean isAvailable() {
        return libreOfficeAvailable;
    }

    /** Async convert a single file */
    @Async
    public void convertAsync(Long fileId) {
        if (!libreOfficeAvailable) {
            ResumeFile file = resumeFileMapper.selectById(fileId);
            if (file != null) {
                markFailed(file, "LibreOffice not available. Install it or configure path in Admin > Brand Config.");
            }
            return;
        }
        ResumeFile file = resumeFileMapper.selectById(fileId);
        if (file != null) {
            convertFile(file);
        }
    }

    /** Convert all pending DOCX/DOC files */
    public int convertAllPending() {
        if (!libreOfficeAvailable) {
            log.warn("LibreOffice not available, skip conversion");
            return 0;
        }

        LambdaQueryWrapper<ResumeFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeFile::getDelFlag, "0")
               .eq(ResumeFile::getPreviewStatus, "PENDING")
               .in(ResumeFile::getFileExt, ".docx", ".doc");

        List<ResumeFile> files = resumeFileMapper.selectList(wrapper);
        if (files.isEmpty()) return 0;

        log.info("Starting batch conversion: {} files", files.size());
        int success = 0;
        for (ResumeFile file : files) {
            if (convertFile(file)) success++;
        }
        log.info("Batch conversion done: {}/{} OK", success, files.size());
        return success;
    }

    private boolean convertFile(ResumeFile file) {
        Path sourcePath = Paths.get(file.getFilePath());
        File sourceFile = sourcePath.toFile();

        if (!sourceFile.exists()) {
            markFailed(file, "Source file missing: " + sourcePath);
            return false;
        }

        Path convertedDir = Paths.get(uploadRoot, "converted");
        try { Files.createDirectories(convertedDir); }
        catch (IOException e) {
            markFailed(file, "Cannot create output dir: " + e.getMessage());
            return false;
        }

        Path tempProfile = null;
        try { tempProfile = Files.createTempDirectory("lo_"); }
        catch (IOException e) {
            markFailed(file, "Cannot create temp dir: " + e.getMessage());
            return false;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                resolvedLibreOfficePath,
                "--headless", "--norestore", "--nofirststartwizard", "--nologo",
                "-env:UserInstallation=file:///" + tempProfile.toAbsolutePath().toString().replace('\\', '/'),
                "--convert-to", "pdf",
                "--outdir", convertedDir.toAbsolutePath().toString(),
                sourceFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                markFailed(file, "Conversion timeout (60s)");
                return false;
            }

            String output = new String(process.getInputStream().readAllBytes());
            if (process.exitValue() != 0) {
                markFailed(file, "Conversion failed (exit=" + process.exitValue() + "): " +
                    output.substring(0, Math.min(200, output.length())));
                return false;
            }

            String sourceName = sourceFile.getName();
            String pdfName = sourceName.substring(0, sourceName.lastIndexOf('.')) + ".pdf";
            Path pdfPath = convertedDir.resolve(pdfName);

            if (!Files.exists(pdfPath)) {
                markFailed(file, "PDF not found after conversion: " + pdfPath);
                return false;
            }

            ResumeFile update = new ResumeFile();
            update.setId(file.getId());
            update.setPreviewStatus("COMPLETED");
            update.setPreviewPath("converted/" + pdfName);
            resumeFileMapper.updateById(update);

            log.info("Converted: fileId={} {} -> {}", file.getId(), file.getOriginalName(), pdfPath);
            return true;

        } catch (Exception e) {
            markFailed(file, "Conversion error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return false;
        } finally {
            try {
                Files.walk(tempProfile)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
            } catch (IOException ignored) {}
        }
    }

    private void markFailed(ResumeFile file, String error) {
        log.warn("Conversion failed: fileId={} name={} -> {}", file.getId(), file.getOriginalName(), error);
        ResumeFile update = new ResumeFile();
        update.setId(file.getId());
        update.setPreviewStatus("FAILED");
        update.setPreviewError(error);
        resumeFileMapper.updateById(update);
    }
}
