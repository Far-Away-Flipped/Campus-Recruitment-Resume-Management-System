package com.atmoto.recruit.biz.common.service;

import com.atmoto.recruit.biz.common.domain.ResumeFile;
import com.atmoto.recruit.biz.common.mapper.ResumeFileMapper;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.UUID;

/**
 * 附件一次性预览 Ticket 服务
 * <p>
 * 安全模型（HR 端 / 学生端共用）：
 * - URL 中只带 ticket，不带 token，因此可以被 window.open / a[target=_blank] 打开
 *   （手机浏览器无法给新标签页附加 Authorization 头）
 * - ticket 一次性：读取后立即消费（invalidate），不能重复使用
 * - ticket 60 秒自动过期
 * - 返回文件流前做路径穿越校验
 * </p>
 * <p>
 * 本类由 {@link com.atmoto.recruit.biz.common.controller.CommonFileController}（HR 端）
 * 与 PortalFileController（学生端）共用，避免复制这段安全相关逻辑。
 * 学生端的 /api/portal/files/preview 是 permitAll，但 ticket 只能由已鉴权的
 * 接口签发，安全强度与 HR 端等价。
 * </p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
public class PreviewTicketService {

    /** 一次性预览ticket缓存（60秒过期） */
    private final Cache<String, Long> previewTicketCache;

    private final ResumeFileMapper resumeFileMapper;

    /** 文件上传根目录，与 application.yml 中 file.upload-root 一致 */
    @Value("${file.upload-root:E:/atmoto-recruit/data}")
    private String uploadRoot;

    public PreviewTicketService(ResumeFileMapper resumeFileMapper,
                                @Qualifier("previewTicketCache") Cache<String, Long> previewTicketCache) {
        this.resumeFileMapper = resumeFileMapper;
        this.previewTicketCache = previewTicketCache;
    }

    /**
     * 为指定文件签发一次性 ticket
     *
     * @param fileId 文件ID（调用方必须先完成归属校验）
     * @return ticket 字符串
     */
    public String issue(Long fileId) {
        String ticket = UUID.randomUUID().toString().replace("-", "");
        previewTicketCache.put(ticket, fileId);
        return ticket;
    }

    /**
     * 消费 ticket 并把文件流写入响应
     *
     * @param ticket   ticket
     * @param response HTTP 响应
     * @param download true=作为附件下载（Content-Disposition: attachment），false=内联预览
     */
    public void consumeAndStream(String ticket, HttpServletResponse response, boolean download) {
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

        // 4. 定位磁盘文件
        File diskFile = resolveExistingFile(file);

        // 5. 返回文件流
        try {
            response.setContentType(getContentType(file.getFileExt()));

            // 文件名编码（中文支持）
            String encodedName = java.net.URLEncoder.encode(
                    file.getOriginalName() == null ? "resume" : file.getOriginalName(), "UTF-8")
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition",
                    (download ? "attachment" : "inline") + "; filename*=UTF-8''" + encodedName);

            try (InputStream is = new FileInputStream(diskFile);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

            log.info("文件{}成功：fileId={}", download ? "下载" : "预览", fileId);
        } catch (IOException e) {
            log.error("文件读取失败：fileId={}, filePath={}", fileId, file.getFilePath(), e);
            throw new BizException(ErrorCode.FILE_PREVIEW_FAILED);
        }
    }

    /**
     * 定位磁盘文件并做路径穿越校验
     * <p>调用方通常已在签发 ticket 前校验过归属，这里只保证"拿到手的路径确实在
     * upload-root 之内"，防止数据库里的路径被篡改后越权读取任意文件。</p>
     * <p>签发 ticket 前也可先调用一次：让"文件已被清理"这类错误在页面内暴露，
     * 而不是等用户跳到新标签页才看到一个 JSON 错误。</p>
     */
    public File resolveExistingFile(ResumeFile file) {
        String filePath = file.getFilePath();
        if (filePath == null || filePath.isEmpty()) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }

        File diskFile = new File(filePath);
        if (!diskFile.isAbsolute()) {
            diskFile = new File(uploadRoot, filePath);
        }
        if (!diskFile.exists()) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND, "文件在磁盘上不存在");
        }

        try {
            String canonicalPath = diskFile.getCanonicalPath();
            String baseCanonical = new File(uploadRoot).getCanonicalPath();
            if (!canonicalPath.startsWith(baseCanonical)) {
                log.warn("路径穿越尝试：requested={}, canonical={}, base={}",
                        filePath, canonicalPath, baseCanonical);
                throw new BizException(ErrorCode.NO_PERMISSION, "非法文件访问");
            }
        } catch (IOException e) {
            log.error("路径校验失败：filePath={}", filePath, e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "文件路径校验失败");
        }

        return diskFile;
    }

    /** 根据文件扩展名返回对应的 Content-Type */
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
}
