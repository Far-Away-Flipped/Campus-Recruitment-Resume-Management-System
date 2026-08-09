package com.atmoto.recruit.biz.admin.controller;

import com.atmoto.recruit.biz.admin.dto.BatchScreenDTO;
import com.atmoto.recruit.biz.admin.dto.ExportDTO;
import com.atmoto.recruit.biz.admin.dto.RemarkDTO;
import com.atmoto.recruit.biz.admin.dto.ResumeQueryDTO;
import com.atmoto.recruit.biz.admin.service.AppHrNoteService;
import com.atmoto.recruit.biz.admin.service.ResumeActionService;
import com.atmoto.recruit.biz.admin.service.ResumeExportService;
import com.atmoto.recruit.biz.admin.service.ResumeQueryService;
import com.atmoto.recruit.biz.admin.vo.*;
import com.atmoto.recruit.biz.common.domain.AppStatusHistory;
import com.atmoto.recruit.biz.common.domain.Application;
import com.atmoto.recruit.biz.common.domain.AuditResumeAccess;
import com.atmoto.recruit.biz.common.domain.ResumeFile;
import com.atmoto.recruit.biz.common.enums.ApplicationStatus;
import com.atmoto.recruit.biz.common.mapper.AppStatusHistoryMapper;
import com.atmoto.recruit.biz.common.mapper.ApplicationMapper;
import com.atmoto.recruit.biz.common.mapper.AuditResumeAccessMapper;
import com.atmoto.recruit.biz.common.mapper.ResumeFileMapper;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.common.core.domain.TableDataInfo;
import com.atmoto.recruit.common.core.page.PageQuery;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.atmoto.recruit.framework.security.context.AdminUserHolder;
import com.atmoto.recruit.system.domain.SysUser;
import com.atmoto.recruit.system.service.ISysUserService;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * HR简历管理 Controller
 * <p>HR端简历多维筛选、详情查看、筛选操作（通过/淘汰/状态流转）、内部备注、附件预览、导出</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/resumes")
public class ResumeAdminController {

    private final ResumeQueryService resumeQueryService;
    private final ResumeActionService resumeActionService;
    private final AppHrNoteService appHrNoteService;
    private final ResumeExportService resumeExportService;
    private final ResumeFileMapper resumeFileMapper;
    private final ISysUserService sysUserService;
    private final ApplicationMapper applicationMapper;
    private final AppStatusHistoryMapper appStatusHistoryMapper;
    private final AuditResumeAccessMapper auditResumeAccessMapper;

    /** 一次性预览ticket缓存（60秒过期，用后即焚） */
    private final Cache<String, Long> previewTicketCache;

    public ResumeAdminController(ResumeQueryService resumeQueryService,
                                  ResumeActionService resumeActionService,
                                  AppHrNoteService appHrNoteService,
                                  ResumeExportService resumeExportService,
                                  ResumeFileMapper resumeFileMapper,
                                  ISysUserService sysUserService,
                                  ApplicationMapper applicationMapper,
                                  AppStatusHistoryMapper appStatusHistoryMapper,
                                  AuditResumeAccessMapper auditResumeAccessMapper,
                                  @Qualifier("previewTicketCache") Cache<String, Long> previewTicketCache) {
        this.resumeQueryService = resumeQueryService;
        this.resumeActionService = resumeActionService;
        this.appHrNoteService = appHrNoteService;
        this.resumeExportService = resumeExportService;
        this.resumeFileMapper = resumeFileMapper;
        this.sysUserService = sysUserService;
        this.applicationMapper = applicationMapper;
        this.appStatusHistoryMapper = appStatusHistoryMapper;
        this.auditResumeAccessMapper = auditResumeAccessMapper;
        this.previewTicketCache = previewTicketCache;
    }

    // ────────── 1. 简历列表多维筛选 ──────────

    /**
     * 多维筛选分页查询简历列表
     * <p>支持岗位ID、状态、关键字、学校、专业、学历、投递时间范围筛选。
     * 非全部数据权限的HR仅能查看自己负责岗位下的投递。</p>
     */
    @GetMapping("/list")
    public AjaxResult list(ResumeQueryDTO queryDTO, PageQuery pageQuery) {
        Long currentUserId = getCurrentUserId();
        boolean hasAllDataScope = hasAllDataScope();
        TableDataInfo dataInfo = resumeQueryService.selectResumeList(
                queryDTO, pageQuery, currentUserId, hasAllDataScope);
        return AjaxResult.page(dataInfo);
    }

    // ────────── 2. 简历详情 ──────────

    /**
     * 查看简历详情
     * <p>含学生基本资料、教育经历、当前快照版本、附件列表</p>
     */
    @GetMapping("/{applicationId}")
    public AjaxResult detail(@PathVariable Long applicationId) {
        Long currentUserId = getCurrentUserId();
        boolean hasAllDataScope = hasAllDataScope();
        ResumeDetailVO detail = resumeQueryService.selectResumeDetail(
                applicationId, currentUserId, hasAllDataScope);
        auditResumeAccess(applicationId, "VIEW_RESUME");
        return AjaxResult.success(detail);
    }

    // ────────── 3. HR内部备注 ──────────

    /**
     * 添加HR内部备注
     */
    @PostMapping("/{applicationId}/remark")
    public AjaxResult addRemark(@PathVariable Long applicationId, @RequestBody RemarkDTO remarkDTO) {
        Long currentUserId = getCurrentUserId();
        String currentUsername = getCurrentUsername();
        appHrNoteService.addRemark(applicationId, remarkDTO, currentUserId, currentUsername);
        return AjaxResult.success("备注添加成功");
    }

    /**
     * 查看备注列表
     */
    @GetMapping("/{applicationId}/remarks")
    public AjaxResult listRemarks(@PathVariable Long applicationId) {
        List<HrNoteVO> remarks = appHrNoteService.listRemarks(applicationId);
        return AjaxResult.success(remarks);
    }

    // ────────── 4. 单人筛选操作 ──────────

    /**
     * 单个筛选通过
     */
    @PutMapping("/{applicationId}/screen-pass")
    public AjaxResult screenPass(@PathVariable Long applicationId) {
        Long currentUserId = getCurrentUserId();
        resumeActionService.screenPass(applicationId, currentUserId);
        auditResumeAccess(applicationId, "SCREEN_PASS");
        return AjaxResult.success("筛选通过成功");
    }

    /**
     * 单个筛选淘汰
     */
    @PutMapping("/{applicationId}/screen-eliminate")
    public AjaxResult screenEliminate(@PathVariable Long applicationId) {
        Long currentUserId = getCurrentUserId();
        resumeActionService.screenEliminate(applicationId, currentUserId);
        auditResumeAccess(applicationId, "SCREEN_ELIMINATE");
        return AjaxResult.success("筛选淘汰成功");
    }

    // ────────── 5. 批量筛选 ──────────

    /**
     * 批量筛选（通过/淘汰）
     * <p>部分失败时：成功的先提交，失败的返回原因清单</p>
     */
    @PostMapping("/batch-screen")
    public AjaxResult batchScreen(@RequestBody BatchScreenDTO dto) {
        Long currentUserId = getCurrentUserId();
        boolean hasAllDataScope = hasAllDataScope();

        List<BatchResultVO> results = resumeActionService.batchScreen(
                dto.getApplicationIds(), dto.getAction(), currentUserId, hasAllDataScope);

        // 统计成功/失败数
        long successCount = results.stream().filter(BatchResultVO::isSuccess).count();
        long failCount = results.stream().filter(r -> !r.isSuccess()).count();

        String msg = String.format("批量操作完成：成功 %d 条, 失败 %d 条", successCount, failCount);
        return AjaxResult.success(msg, results);
    }

    // ────────── 5.1 状态流转 ──────────

    /** 状态流转规则：当前状态 -> 允许变更为的状态列表 */
    private static final Map<String, List<String>> STATUS_TRANSITIONS = Map.ofEntries(
            Map.entry("PENDING_SCREEN", List.of("SCREEN_PASSED", "ELIMINATED")),
            Map.entry("SCREEN_PASSED", List.of("PENDING_INTERVIEW", "ELIMINATED")),
            Map.entry("PENDING_INTERVIEW", List.of("IN_INTERVIEW", "ELIMINATED")),
            Map.entry("IN_INTERVIEW", List.of("INTERVIEW_PASSED", "ELIMINATED")),
            Map.entry("INTERVIEW_PASSED", List.of("PENDING_OFFER", "ELIMINATED")),
            Map.entry("PENDING_OFFER", List.of("OFFER_SENT", "ELIMINATED")),
            Map.entry("OFFER_SENT", List.of("ACCEPTED", "REJECTED")),
            Map.entry("ACCEPTED", List.of("ONBOARDED"))
    );

    /**
     * 手动变更候选人状态
     * <p>根据预设的状态流转规则校验合法性，
     * 更新 app_application.status（乐观锁），写入 app_status_history</p>
     *
     * @param applicationId 投递记录ID
     * @param body          包含 status 字段的 JSON（新状态码）
     * @return 操作结果
     */
    @PutMapping("/{applicationId}/status")
    public AjaxResult changeStatus(@PathVariable Long applicationId,
                                   @RequestBody Map<String, String> body) {
        String newStatusCode = body.get("status");
        if (newStatusCode == null || newStatusCode.isBlank()) {
            return AjaxResult.error("状态参数不能为空");
        }

        // 校验新状态是否为合法的枚举值
        ApplicationStatus targetStatus;
        try {
            targetStatus = ApplicationStatus.fromCode(newStatusCode);
        } catch (IllegalArgumentException e) {
            return AjaxResult.error("无效的状态值：" + newStatusCode);
        }

        Long currentUserId = getCurrentUserId();
        boolean hasAllDataScope = hasAllDataScope();

        // 使用 MyBatis-Plus 内置 selectById（正确映射 @TableField("version_no") → version）
        Application app = applicationMapper.selectById(applicationId);
        if (app == null) {
            return AjaxResult.error("投递记录不存在");
        }

        String currentStatus = app.getStatus();

        // 校验状态流转规则：当前状态 -> 目标状态是否在允许列表中
        List<String> allowed = STATUS_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(newStatusCode)) {
            ApplicationStatus currentStatusEnum;
            try {
                currentStatusEnum = ApplicationStatus.fromCode(currentStatus);
            } catch (IllegalArgumentException e) {
                currentStatusEnum = null;
            }
            String currentLabel = currentStatusEnum != null
                    ? currentStatusEnum.getLabel() : currentStatus;
            return AjaxResult.error("当前状态「" + currentLabel
                    + "」不允许变更为「" + targetStatus.getLabel() + "」");
        }

        // 乐观锁更新
        Integer currentVersion = app.getVersion();
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Application> updateWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        updateWrapper.eq(Application::getApplicationId, applicationId)
                .eq(Application::getVersion, currentVersion)
                .eq(Application::getStatus, currentStatus)
                .set(Application::getStatus, newStatusCode)
                .set(Application::getVersion, currentVersion != null ? currentVersion + 1 : 1);

        int affectedRows = applicationMapper.update(null, updateWrapper);
        if (affectedRows == 0) {
            log.warn("状态变更乐观锁冲突：applicationId={}, version={}", applicationId, currentVersion);
            return AjaxResult.error("该简历已被其他同事操作，请刷新后重试");
        }

        // 记录状态变更历史
        AppStatusHistory history = new AppStatusHistory();
        history.setApplicationId(applicationId);
        history.setFromStatus(currentStatus);
        history.setToStatus(newStatusCode);
        history.setOperatorType("HR");
        history.setOperatorId(currentUserId);
        history.setRemark("手动状态变更：" + currentStatus + " -> " + newStatusCode);
        history.setOperateTime(LocalDateTime.now());
        appStatusHistoryMapper.insert(history);

        log.info("状态变更成功：applicationId={}, from={}, to={}, operator={}",
                applicationId, currentStatus, newStatusCode, currentUserId);
        return AjaxResult.success("状态变更成功");
    }

    // ────────── 6. 附件列表 ──────────

    /**
     * 查看投递记录的附件列表（需鉴权：HR必须有权查看该申请）
     */
    @GetMapping("/{applicationId}/attachments")
    public AjaxResult attachments(@PathVariable Long applicationId) {
        // 通过resumeQueryService的详情查询做鉴权，同时获取数据
        Long currentUserId = getCurrentUserId();
        boolean hasAllDataScope = hasAllDataScope();
        ResumeDetailVO detail = resumeQueryService.selectResumeDetail(
                applicationId, currentUserId, hasAllDataScope);
        return AjaxResult.success(detail.getAttachments());
    }

    // ────────── 7. 附件一次性预览Ticket ──────────

    /**
     * 生成附件预览一次性Ticket
     * <p>生成UUID ticket存入Caffeine（60s），返回给前端。
     * 前端用该ticket调 /api/common/file/preview 获取文件流。
     * URL中绝不携带token。</p>
     */
    @PostMapping("/{applicationId}/attachments/{fileId}/ticket")
    public AjaxResult generatePreviewTicket(@PathVariable Long applicationId,
                                            @PathVariable Long fileId) {
        // 先校验HR有权查看此申请
        Long currentUserId = getCurrentUserId();
        boolean hasAllDataScope = hasAllDataScope();
        resumeQueryService.selectResumeDetail(applicationId, currentUserId, hasAllDataScope);

        // 校验文件存在且属于此申请对应的学生
        ResumeFile file = resumeFileMapper.selectById(fileId);
        if (file == null) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
        ResumeDetailVO detail = resumeQueryService.selectResumeDetail(
                applicationId, currentUserId, hasAllDataScope);
        if (!detail.getStudentId().equals(file.getStudentId())) {
            throw new BizException(ErrorCode.NO_PERMISSION, "该附件不属于此投递记录");
        }

        // 校验文件在磁盘上实际存在
        String filePath = file.getFilePath();
        if (filePath == null || filePath.isEmpty()) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND, "文件路径无效");
        }
        java.io.File diskFile = new java.io.File(filePath);
        if (!diskFile.isAbsolute()) {
            diskFile = new java.io.File("E:/atmoto-recruit/data", filePath);
        }
        if (!diskFile.exists()) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND, "文件在磁盘上不存在，可能已被清理");
        }

        // 生成一次性ticket
        String ticket = UUID.randomUUID().toString().replace("-", "");
        previewTicketCache.put(ticket, fileId);
        log.info("生成预览ticket：applicationId={}, fileId={}, ticket={}", applicationId, fileId, ticket);

        PreviewTicketVO vo = new PreviewTicketVO();
        vo.setTicket(ticket);
        return AjaxResult.success(vo);
    }

    /**
     * 直接预览附件（与学生端一致，返回文件流，无 ticket 机制）
     * <p>鉴权：HR 必须是该投递记录的权限持有者。Content-Type 正确设置，浏览器可内嵌预览。</p>
     */
    @GetMapping("/{applicationId}/attachments/{fileId}/preview")
    public ResponseEntity<Resource> previewFile(
            @PathVariable Long applicationId,
            @PathVariable Long fileId) {
        // 鉴权
        Long currentUserId = getCurrentUserId();
        boolean hasAllDataScope = hasAllDataScope();
        ResumeDetailVO detail = resumeQueryService.selectResumeDetail(
                applicationId, currentUserId, hasAllDataScope);

        // 校验文件归属
        ResumeFile file = resumeFileMapper.selectById(fileId);
        if (file == null) throw new BizException(ErrorCode.FILE_NOT_FOUND);
        if (!detail.getStudentId().equals(file.getStudentId()))
            throw new BizException(ErrorCode.NO_PERMISSION);

        // 定位磁盘文件
        String fp = file.getFilePath();
        if (fp == null || fp.isEmpty()) throw new BizException(ErrorCode.FILE_NOT_FOUND);
        java.io.File diskFile = new java.io.File(fp);
        if (!diskFile.isAbsolute()) diskFile = new java.io.File("E:/atmoto-recruit/data", fp);
        if (!diskFile.exists()) throw new BizException(ErrorCode.FILE_NOT_FOUND, "文件不存在");

        // 根据扩展名确定 Content-Type
        String ext = file.getFileExt();
        String contentType = switch (ext != null ? ext.toLowerCase() : "") {
            case ".pdf" -> "application/pdf";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            default -> "application/octet-stream";
        };
        // PDF 转换产物优先
        if (("COMPLETED".equals(file.getPreviewStatus()) || "READY".equals(file.getPreviewStatus()))
                && file.getPreviewPath() != null) {
            java.io.File pdfFile = new java.io.File("E:/atmoto-recruit/data", file.getPreviewPath());
            if (pdfFile.exists()) {
                diskFile = pdfFile;
                contentType = "application/pdf";
            }
        }

        Resource resource = new FileSystemResource(diskFile);
        String encodedName;
        try {
            encodedName = URLEncoder.encode(file.getOriginalName(), "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            encodedName = "file";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                .body(resource);
    }

    // ────────── 8. 导出Excel ──────────

    /**
     * 导出简历列表为Excel
     * <p>返回文件下载流。日累计上限2000条。</p>
     */
    @PostMapping("/export")
    public AjaxResult exportResumes(@RequestBody ExportDTO dto, HttpServletResponse response) {
        Long currentUserId = getCurrentUserId();
        String currentUsername = getCurrentUsername();
        boolean hasAllDataScope = hasAllDataScope();

        // 调用导出服务生成文件
        String filePath = resumeExportService.exportResumeList(
                dto.getApplicationIds(), currentUserId, currentUsername, hasAllDataScope);

        // 返回文件路径给前端下载
        return AjaxResult.success("导出成功", filePath);
    }

    // ────────── 文件下载 ──────────

    /**
     * 下载导出文件
     * <p>根据文件路径返回文件流</p>
     */
    @GetMapping("/export/download")
    public void downloadExportFile(@RequestParam("path") String filePath,
                                   HttpServletResponse response) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND, "导出文件不存在或已过期");
        }

        // 校验路径安全性：必须在合法目录下
        String exportDir = System.getProperty("java.io.tmpdir") + File.separator + "recruit-export";
        if (!file.getCanonicalPath().startsWith(new File(exportDir).getCanonicalPath())) {
            throw new BizException(ErrorCode.NO_PERMISSION, "非法文件路径");
        }

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String encodedFileName = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" + encodedFileName);

            try (InputStream is = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
        } catch (IOException e) {
            log.error("文件下载失败：filePath={}", filePath, e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "文件下载失败");
        }
    }

    // ────────── 辅助方法 ──────────

    /**
     * 从ThreadLocal获取当前HR用户ID
     */
    private Long getCurrentUserId() {
        Long userId = AdminUserHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 从ThreadLocal获取当前HR用户名
     */
    private String getCurrentUsername() {
        String username = AdminUserHolder.getUsername();
        if (username == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return username;
    }

    /**
     * 判断当前HR是否有全部数据权限
     * <p>
     * 当前阶段简化实现：查询 sys_user 表，判断 user_type 是否为 "sys_admin"。
     * 后续里程碑中接入 RuoYi 的 DataScope 注解体系。
     * </p>
     */
    private boolean hasAllDataScope() {
        Long userId = AdminUserHolder.getUserId();
        if (userId == null) {
            return false;
        }
        try {
            SysUser sysUser = sysUserService.selectUserById(userId);
            if (sysUser != null && "sys_admin".equals(sysUser.getUserType())) {
                return true;
            }
        } catch (Exception e) {
            log.debug("查询用户类型失败，默认非全部权限：userId={}", userId);
        }
        return false;
    }

    /**
     * 写入简历访问审计日志（个保法合规：所有对个人简历数据的查看/导出/下载/筛选操作强制记录）
     */
    private void auditResumeAccess(Long applicationId, String operationType) {
        Application app = applicationMapper.selectById(applicationId);
        if (app == null) {
            log.warn("审计失败：applicationId={} 不存在", applicationId);
            return;
        }
        Long operatorId = getCurrentUserId();
        String operatorName = getCurrentUsername();
        AuditResumeAccess audit = new AuditResumeAccess();
        audit.setOperatorId(operatorId);
        audit.setOperatorName(operatorName != null ? operatorName : "system");
        audit.setTargetStudentId(app.getStudentId());
        audit.setTargetStudentName(app.getSnapshotName() != null ? app.getSnapshotName() : "");
        audit.setTargetApplicationId(applicationId);
        audit.setOperationType(operationType);
        audit.setCreateTime(LocalDateTime.now());
        auditResumeAccessMapper.insert(audit);
        log.info("审计日志已写入: applicationId={}, type={}", applicationId, operationType);
    }
}
