package com.atmoto.recruit.biz.portal.controller;

import com.atmoto.recruit.biz.common.domain.ResumeFile;
import com.atmoto.recruit.biz.common.mapper.ResumeFileMapper;
import com.atmoto.recruit.biz.file.DocumentConversionService;
import com.atmoto.recruit.biz.file.FileValidator;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.atmoto.recruit.framework.security.context.PortalUserHolder;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * 学生端文件 Controller
 * <p>
 * 前缀：/api/portal/files，提供简历附件上传、列表、删除、预览
 * 铁律：所有操作的 studentId 从 PortalUserHolder 获取，绝不从请求参数传入
 * </p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portal/files")
public class PortalFileController {

    private final ResumeFileMapper resumeFileMapper;
    private final DocumentConversionService conversionService;

    @Value("${file.upload-root}")
    private String uploadRoot;

    /**
     * 上传简历附件
     * <p>五道校验(扩展名+MIME+大小+魔数+DOCX结构) → UUID重命名存盘 → 写 stu_resume_file</p>
     */
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file) {
        Long studentId = PortalUserHolder.get();

        // 五道校验
        FileValidator.validate(file);

        // 生成存储文件名：UUID + 原始扩展名
        String originalName = file.getOriginalFilename();
        String ext = originalName != null
                ? originalName.substring(originalName.lastIndexOf('.'))
                : ".dat";
        String storedName = UUID.randomUUID().toString() + ext;

        // 按日期分目录存储
        String dateDir = java.time.LocalDate.now().toString().replace("-", "/");
        Path dirPath = Paths.get(uploadRoot, "resume", dateDir);
        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            log.error("创建存储目录失败：{}", dirPath, e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "文件存储目录创建失败");
        }

        // 写入磁盘
        Path filePath = dirPath.resolve(storedName);
        try {
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            log.error("文件写入失败：{}", filePath, e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
        }

        // 写数据库
        ResumeFile resumeFile = new ResumeFile();
        resumeFile.setStudentId(studentId);
        resumeFile.setOriginalName(originalName);
        resumeFile.setFilePath(filePath.toString());
        resumeFile.setFileExt(ext.toLowerCase());
        resumeFile.setFileSize(file.getSize());

        // Word 类型标记预览状态为 PENDING，并异步触发转换
        if (".docx".equalsIgnoreCase(ext) || ".doc".equalsIgnoreCase(ext)) {
            resumeFile.setPreviewStatus("PENDING");
        } else {
            resumeFile.setPreviewStatus("NONE");
        }

        resumeFileMapper.insert(resumeFile);

        // 上传成功后异步触发文档转换（不阻塞上传响应）
        if ("PENDING".equals(resumeFile.getPreviewStatus())) {
            conversionService.convertAsync(resumeFile.getId());
        }

        log.info("文件上传成功：studentId={}, originalName={}, storedName={}, size={}",
                studentId, originalName, storedName, file.getSize());

        return AjaxResult.success("上传成功", resumeFile);
    }

    /**
     * 获取本人简历附件列表
     */
    @GetMapping
    public AjaxResult list() {
        Long studentId = PortalUserHolder.get();
        List<ResumeFile> files = resumeFileMapper.selectList(
                new LambdaQueryWrapper<ResumeFile>()
                        .eq(ResumeFile::getStudentId, studentId)
                        .orderByDesc(ResumeFile::getCreateTime));
        return AjaxResult.success(files);
    }

    /**
     * 删除简历附件
     */
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        Long studentId = PortalUserHolder.get();

        // 越权保护：必须先查到属于当前学生的记录
        ResumeFile file = resumeFileMapper.selectOne(
                new LambdaQueryWrapper<ResumeFile>()
                        .eq(ResumeFile::getId, id)
                        .eq(ResumeFile::getStudentId, studentId));
        if (file == null) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }

        // 删除磁盘上的文件
        try {
            Path filePath = Paths.get(file.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("磁盘文件删除失败（忽略继续）：{}", file.getFilePath(), e);
        }

        // 删除数据库记录（逻辑删除，MyBatis-Plus 自动处理）
        resumeFileMapper.deleteById(id);

        log.info("文件删除成功：studentId={}, fileId={}, originalName={}",
                studentId, id, file.getOriginalName());

        return AjaxResult.success("删除成功");
    }

    /**
     * 预览简历附件（鉴权返回文件流）
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> preview(@PathVariable Long id) {
        Long studentId = PortalUserHolder.get();
        // 安全：未认证用户直接拒绝，防止 MyBatis-Plus null 条件被跳过导致越权
        if (studentId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        // 越权保护
        ResumeFile file = resumeFileMapper.selectOne(
                new LambdaQueryWrapper<ResumeFile>()
                        .eq(ResumeFile::getId, id)
                        .eq(ResumeFile::getStudentId, studentId));
        if (file == null) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }

        Path filePath = Paths.get(file.getFilePath());
        if (!Files.exists(filePath)) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }

        // 根据扩展名确定 Content-Type
        String contentType = switch (file.getFileExt().toLowerCase()) {
            case ".pdf" -> "application/pdf";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };

        // 若为 Word 文件且已有转换完成的 PDF，优先返回 PDF 预览
        Path previewPath = filePath;
        String previewContentType = contentType;
        if ((".docx".equalsIgnoreCase(file.getFileExt()) || ".doc".equalsIgnoreCase(file.getFileExt()))
                && "COMPLETED".equals(file.getPreviewStatus())
                && file.getPreviewPath() != null) {
            Path convertedPath = Paths.get(uploadRoot, file.getPreviewPath());
            if (Files.exists(convertedPath)) {
                previewPath = convertedPath;
                previewContentType = "application/pdf";
            }
        }

        Resource resource = new FileSystemResource(previewPath);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(previewContentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + file.getOriginalName() + "\"")
                .body(resource);
    }
}
