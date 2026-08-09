package com.atmoto.recruit.biz.admin.controller;

import com.atmoto.recruit.biz.admin.vo.*;
import com.atmoto.recruit.biz.common.domain.AuditResumeAccess;
import com.atmoto.recruit.biz.common.domain.Student;
import com.atmoto.recruit.biz.common.mapper.AuditResumeAccessMapper;
import com.atmoto.recruit.biz.common.mapper.StudentMapper;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.common.core.domain.TableDataInfo;
import com.atmoto.recruit.common.utils.IpUtils;
import com.atmoto.recruit.framework.security.context.AdminUserHolder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * 学生用户管理 Controller
 * <p>管理后台学生用户管理：分页查询（含排序）、详情查看、启用/禁用、逻辑删除</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/students")
public class StudentManageController {

    private final StudentMapper studentMapper;
    private final AuditResumeAccessMapper auditResumeAccessMapper;

    private static final Set<String> VALID_STATUSES = Set.of("ACTIVE", "DISABLED");

    /** 排序字段白名单 → SQL 列名映射（防 SQL 注入，仅允许预定义列名） */
    private static final Map<String, String> SORT_COLUMN_MAP = Map.of(
        "createTime", "s.create_time",
        "schoolName", "edu.school_name",
        "major", "edu.major"
    );

    /**
     * 分页查询学生用户列表（含排序）
     *
     * @param sortField 排序字段（schoolName/major/createTime），非法值降级为 createTime
     * @param sortOrder 排序方向（asc/desc），非法值降级为 desc
     */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "20") int pageSize,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false, defaultValue = "createTime") String sortField,
                           @RequestParam(required = false, defaultValue = "desc") String sortOrder) {
        // 排序字段白名单校验 + 映射（非法值静默降级）
        String sortColumn = SORT_COLUMN_MAP.getOrDefault(sortField, "s.create_time");
        String safeSortOrder = "asc".equalsIgnoreCase(sortOrder) ? "asc" : "desc";

        Page<StudentManageVO> page = new Page<>(pageNum, pageSize);
        IPage<StudentManageVO> result = studentMapper.selectStudentPage(page, keyword, status, sortColumn, safeSortOrder);

        // 手机号脱敏
        for (StudentManageVO vo : result.getRecords()) {
            if (vo.getPhone() != null && vo.getPhone().length() >= 7) {
                vo.setPhone(vo.getPhone().substring(0, 3) + "****" + vo.getPhone().substring(7));
            }
        }
        return AjaxResult.page(TableDataInfo.of((int) result.getTotal(), result.getRecords()));
    }

    /**
     * 查看学生详情（基本信息 + 教育经历 + 实习/项目 + 简历附件 + 投递历史）
     * <p>手机号返回完整值。每次查看写入审计日志（个保法合规）</p>
     */
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id, HttpServletRequest request) {
        StudentDetailVO detail = studentMapper.selectStudentDetail(id);
        if (detail == null) {
            return AjaxResult.error("学生账号不存在");
        }
        // 状态中文标签
        detail.setStatusLabel("ACTIVE".equals(detail.getStatus()) ? "正常" : "已禁用");

        detail.setEducations(studentMapper.selectEducationsByStudentId(id));

        // 实习/项目经历 — recordType 映射为中文标签
        java.util.List<InternshipBriefVO> internships = studentMapper.selectInternshipsByStudentId(id);
        for (InternshipBriefVO intern : internships) {
            intern.setRecordTypeLabel("I".equals(intern.getRecordType()) ? "实习经历" : "项目经历");
        }
        detail.setInternships(internships);
        detail.setResumeFiles(studentMapper.selectResumeFilesByStudentId(id));

        // 投递历史 — 状态码映射为中文标签
        java.util.List<ApplicationBriefVO> applications = studentMapper.selectApplicationsByStudentId(id);
        for (ApplicationBriefVO app : applications) {
            app.setStatusLabel(applicationStatusLabel(app.getStatus()));
        }
        detail.setApplications(applications);

        // 审计埋点（不阻塞主流程）
        auditStudentAccess(detail, request);

        return AjaxResult.success(detail);
    }

    /** 写入学生详情查看审计日志 */
    private void auditStudentAccess(StudentDetailVO detail, HttpServletRequest request) {
        try {
            Long operatorId = AdminUserHolder.getUserId();
            String operatorName = AdminUserHolder.getUsername();
            if (operatorId == null) return;

            String studentName = detail.getRealName() != null ? detail.getRealName() : "";

            AuditResumeAccess audit = new AuditResumeAccess();
            audit.setOperatorId(operatorId);
            audit.setOperatorName(operatorName != null ? operatorName : "system");
            audit.setTargetStudentId(detail.getStudentId());
            audit.setTargetStudentName(studentName);
            audit.setOperationType("VIEW_STUDENT_DETAIL");
            audit.setOperationDetail("查看学生详情");
            audit.setIpAddress(IpUtils.getClientIp(request));
            audit.setUserAgent(request.getHeader("User-Agent"));
            audit.setCreateTime(LocalDateTime.now());
            auditResumeAccessMapper.insert(audit);
        } catch (Exception e) {
            log.warn("学生详情审计写入失败（不影响主流程）：studentId={}", detail.getStudentId(), e);
        }
    }

    /**
     * 启用/禁用学生账号
     */
    @PutMapping("/{id}/status")
    public AjaxResult updateStatus(@PathVariable Long id,
                                   @RequestParam String status) {
        if (status == null || !VALID_STATUSES.contains(status)) {
            return AjaxResult.error("无效的状态值，仅支持 ACTIVE / DISABLED");
        }
        Student student = studentMapper.selectById(id);
        if (student == null) {
            return AjaxResult.error("学生账号不存在");
        }
        LambdaUpdateWrapper<Student> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Student::getStudentId, id)
                .set(Student::getStatus, status);
        studentMapper.update(null, wrapper);
        String action = "ACTIVE".equals(status) ? "启用" : "禁用";
        log.info("学生账号{}成功：studentId={}, phone={}", action, id, student.getPhone());
        return AjaxResult.success(action + "成功");
    }

    /**
     * 逻辑删除学生账号
     */
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            return AjaxResult.error("学生账号不存在");
        }
        int rows = studentMapper.deleteById(id);
        if (rows > 0) {
            log.info("学生账号逻辑删除成功：studentId={}, phone={}", id, student.getPhone());
            return AjaxResult.success("删除成功");
        }
        return AjaxResult.error("删除失败");
    }

    /** 投递状态码 → 中文标签 */
    private String applicationStatusLabel(String status) {
        if (status == null) return "-";
        return switch (status) {
            case "PENDING_SCREEN" -> "待筛选";
            case "SCREEN_PASSED" -> "筛选通过";
            case "ELIMINATED" -> "已淘汰";
            case "PENDING_INTERVIEW" -> "待面试";
            case "IN_INTERVIEW" -> "面试中";
            case "INTERVIEW_PASSED" -> "面试通过";
            case "PENDING_OFFER" -> "待录用";
            case "OFFER_SENT" -> "已发Offer";
            case "ACCEPTED" -> "已接受";
            case "REJECTED" -> "已拒绝";
            case "ONBOARDED" -> "已入职";
            default -> status;
        };
    }
}
