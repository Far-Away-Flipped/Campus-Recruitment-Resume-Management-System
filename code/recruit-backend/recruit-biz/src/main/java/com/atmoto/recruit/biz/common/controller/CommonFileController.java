package com.atmoto.recruit.biz.common.controller;

import com.atmoto.recruit.biz.common.domain.ResumeFile;
import com.atmoto.recruit.biz.common.mapper.ResumeFileMapper;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;

/**
 * 公共文件预览 Controller（S-11 安全修复）
 * <p>通过一次性ticket返回文件流，URL中不带token。
 * ticket由HR端生成（60秒有效），用后即焚。</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequestMapping("/api/common/file")
public class CommonFileController {

    private final ResumeFileMapper resumeFileMapper;

    /** 一次性预览ticket缓存（60秒过期） */
    private final Cache<String, Long> previewTicketCache;

    /** 文件上传根目录，与 application.yml 中 file.upload-root 一致 */
    @Value("${file.upload-root:E:/atmoto-recruit/data}")
    private String uploadRoot;

    public CommonFileController(ResumeFileMapper resumeFileMapper,
                                @Qualifier("previewTicketCache") Cache<String, Long> previewTicketCache) {
        this.resumeFileMapper = resumeFileMapper;
        this.previewTicketCache = previewTicketCache;
    }

    /**
     * 通过一次性ticket预览文件
     * <p>
     * 安全设计：
     * - URL中不带token，仅凭ticket鉴权
     * - ticket是一次性的：读取后立即消费（删除）
     * - ticket 60秒自动过期
     * - 路径穿越防护
     * </p>
     *
     * @param ticket   一次性预览ticket
     * @param response HTTP响应
     */
    @GetMapping("/preview")
    public void preview(@RequestParam("ticket") String ticket, HttpServletResponse response) {
        // 1. 校验ticket存在
        Long fileId = previewTicketCache.getIfPresent(ticket);
        if (fileId == null) {
            throw new BizException(ErrorCode.TOKEN_INVALID, "预览链接已过期或已被使用，请重新获取");
        }

        // 2. 消费ticket（用后即焚，防止链接被重复使用）
        previewTicketCache.invalidate(ticket);

        // 3. 查询文件记录
        ResumeFile file = resumeFileMapper.selectById(fileId);
        if (file == null) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }

        // 4. 路径穿越防护 + 文件定位
        String filePath = file.getFilePath();
        if (filePath == null || filePath.isEmpty()) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }

        File diskFile = new File(filePath);
        // If path is relative, prepend upload root
        if (!diskFile.isAbsolute()) {
            diskFile = new File(uploadRoot, filePath);
        }

        if (!diskFile.exists()) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND, "文件在磁盘上不存在");
        }

        // 规范化路径，防止 .. 穿越
        try {
            String canonicalPath = diskFile.getCanonicalPath();
            String baseCanonical = new File(uploadRoot).getCanonicalPath();
            if (!canonicalPath.startsWith(baseCanonical)) {
                log.warn("路径穿越尝试：requested={}, canonical={}, base={}", filePath, canonicalPath, baseCanonical);
                throw new BizException(ErrorCode.NO_PERMISSION, "非法文件访问");
            }
        } catch (IOException e) {
            log.error("路径校验失败：filePath={}", filePath, e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "文件路径校验失败");
        }

        // 5. 返回文件流
        try {
            // 根据扩展名设置Content-Type
            String ext = file.getFileExt();
            String contentType = getContentType(ext);
            response.setContentType(contentType);

            // 文件名编码（中文支持）
            String encodedName = java.net.URLEncoder.encode(file.getOriginalName(), "UTF-8")
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition",
                    "inline; filename*=UTF-8''" + encodedName);

            // 写出文件流
            try (InputStream is = new FileInputStream(diskFile);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

            log.info("文件预览成功：fileId={}, ticket={}", fileId, ticket);
        } catch (IOException e) {
            log.error("文件预览读取失败：fileId={}, filePath={}", fileId, filePath, e);
            throw new BizException(ErrorCode.FILE_PREVIEW_FAILED);
        }
    }

    /**
     * 根据文件扩展名返回对应的Content-Type
     */
    private String getContentType(String ext) {
        if (ext == null) return "application/octet-stream";
        switch (ext.toLowerCase()) {
            case ".pdf":
                return "application/pdf";
            case ".doc":
                return "application/msword";
            case ".docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * 获取上传文件的基准目录
     * <p>与文件上传配置保持一致，用于路径穿越校验</p>
     */
    private String getUploadBaseDir() {
        return uploadRoot;
    }
}
