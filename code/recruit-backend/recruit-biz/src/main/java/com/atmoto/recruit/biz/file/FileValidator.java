package com.atmoto.recruit.biz.file;

import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 文件上传校验器 —— 五道安全防线
 * <p>
 * 1. 扩展名白名单：仅允许 .pdf / .doc / .docx
 * 2. MIME 类型辅助校验
 * 3. 文件大小上限 10MB
 * 4. 魔数（Magic Number）二进制校验
 * 5. DOCX 文件额外校验 ZIP 结构的完整性（必须包含 word/document.xml）
 * </p>
 */
@Slf4j
public class FileValidator {

    /** 允许的扩展名白名单 */
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(".pdf", ".doc", ".docx"));

    /** 文件大小上限：10MB */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    /** 魔数常量 */
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};           // %PDF
    private static final byte[] DOC_MAGIC = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}; // D0 CF 11 E0 (OLE2)
    private static final byte[] DOCX_MAGIC = {0x50, 0x4B, 0x03, 0x04};         // PK..

    /**
     * 执行五道校验，任一不通过立即抛出 BizException
     *
     * @param file MultipartFile 上传文件
     */
    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();

        // ── 第1道：扩展名白名单 ──
        String ext = getFileExtension(originalFilename);
        if (ext == null || !ALLOWED_EXTENSIONS.contains(ext)) {
            log.warn("文件扩展名不在白名单：{}", originalFilename);
            throw new BizException(ErrorCode.FILE_FORMAT_UNSUPPORTED);
        }

        // ── 第2道：MIME 类型辅助校验 ──
        String contentType = file.getContentType();
        if (contentType != null && !isAllowedMime(contentType, ext)) {
            log.warn("MIME类型不匹配：{} (ext={}, mime={})", originalFilename, ext, contentType);
            // MIME 仅为辅助因子，不直接拒绝，仅记录警告
        }

        // ── 第3道：文件大小上限 ──
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("文件大小超限：{} (size={})", originalFilename, file.getSize());
            throw new BizException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        // ── 第4道：魔数二进制校验 ──
        byte[] header = readFileHeader(file);
        if (header == null || !matchMagic(header, ext)) {
            log.warn("文件魔数不匹配：{} (ext={})", originalFilename, ext);
            throw new BizException(ErrorCode.FILE_MAGIC_MISMATCH);
        }

        // ── 第5道：DOCX 额外 ZIP 结构完整性校验 ──
        if (".docx".equals(ext)) {
            if (!validateDocxStructure(file)) {
                log.warn("DOCX ZIP结构校验失败：{}", originalFilename);
                throw new BizException(ErrorCode.FILE_FORMAT_UNSUPPORTED,
                        "DOCX 文件结构损坏，请重新保存后上传");
            }
        }

        log.info("文件校验通过：{}", originalFilename);
    }

    // ────────────────── 内部辅助方法 ──────────────────

    /**
     * 获取文件扩展名（小写，含点号）
     */
    private static String getFileExtension(String filename) {
        if (filename == null) return null;
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) return null;
        return filename.substring(dotIndex).toLowerCase();
    }

    /**
     * 检查 MIME 类型是否与扩展名基本匹配（辅助因子，不严格拒绝）
     */
    private static boolean isAllowedMime(String mime, String ext) {
        return switch (ext) {
            case ".pdf" -> mime.equals("application/pdf") || mime.startsWith("application/");
            case ".doc" -> mime.equals("application/msword") || mime.startsWith("application/");
            case ".docx" -> mime.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    || mime.startsWith("application/");
            default -> true;
        };
    }

    /**
     * 读取文件头4字节用于魔数校验
     */
    private static byte[] readFileHeader(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[4];
            int bytesRead = is.read(header);
            if (bytesRead < 4) {
                return null;
            }
            return header;
        } catch (IOException e) {
            log.error("读取文件头失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 魔数匹配
     */
    private static boolean matchMagic(byte[] header, String ext) {
        return switch (ext) {
            case ".pdf" -> startsWith(header, PDF_MAGIC);
            case ".doc" -> startsWith(header, DOC_MAGIC);
            case ".docx" -> startsWith(header, DOCX_MAGIC);
            default -> false;
        };
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    /**
     * 校验 DOCX 文件的 ZIP 结构完整性
     * <p>DOCX 本质是 ZIP 包，必须包含 word/document.xml 作为主文档</p>
     */
    private static boolean validateDocxStructure(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             ZipInputStream zis = new ZipInputStream(is)) {

            boolean hasDocumentXml = false;
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    hasDocumentXml = true;
                    break;
                }
                zis.closeEntry();
            }
            return hasDocumentXml;
        } catch (Exception e) {
            log.warn("DOCX ZIP结构校验异常：{}", e.getMessage());
            return false;
        }
    }
}
