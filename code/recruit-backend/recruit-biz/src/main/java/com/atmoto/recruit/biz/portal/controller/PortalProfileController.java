package com.atmoto.recruit.biz.portal.controller;

import com.atmoto.recruit.biz.common.domain.StudentActivity;
import com.atmoto.recruit.biz.common.domain.StudentCertificate;
import com.atmoto.recruit.biz.common.domain.StudentEducation;
import com.atmoto.recruit.biz.common.domain.StudentInternship;
import com.atmoto.recruit.biz.common.domain.StudentProfile;
import com.atmoto.recruit.biz.portal.service.PortalProfileService;
import com.atmoto.recruit.common.core.domain.AjaxResult;
import com.atmoto.recruit.framework.security.context.PortalUserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学生端资料 Controller
 * <p>
 * 前缀：/api/portal/profile，提供基本资料、教育经历、实习/项目经历、技能证书、社团经历的 CRUD
 * 铁律：所有操作的 studentId 从 PortalUserHolder 获取，绝不从请求参数传入
 * </p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portal/profile")
public class PortalProfileController {

    private final PortalProfileService portalProfileService;

    /**
     * 获取本人基本资料
     */
    @GetMapping
    public AjaxResult getProfile() {
        Long studentId = PortalUserHolder.get();
        StudentProfile profile = portalProfileService.getProfile(studentId);
        return AjaxResult.success(profile);
    }

    /**
     * 更新基本资料
     */
    @PutMapping
    public AjaxResult updateProfile(@RequestBody StudentProfile profile) {
        Long studentId = PortalUserHolder.get();
        portalProfileService.updateProfile(studentId, profile);
        return AjaxResult.success("资料更新成功");
    }

    /**
     * 获取教育经历列表
     */
    @GetMapping("/education")
    public AjaxResult getEducations() {
        Long studentId = PortalUserHolder.get();
        List<StudentEducation> educations = portalProfileService.getEducations(studentId);
        return AjaxResult.success(educations);
    }

    /**
     * 新增教育经历
     */
    @PostMapping("/education")
    public AjaxResult addEducation(@RequestBody StudentEducation education) {
        Long studentId = PortalUserHolder.get();
        StudentEducation saved = portalProfileService.addEducation(studentId, education);
        return AjaxResult.success("教育经历添加成功", saved);
    }

    /**
     * 更新教育经历
     */
    @PutMapping("/education/{id}")
    public AjaxResult updateEducation(@PathVariable Long id, @RequestBody StudentEducation education) {
        Long studentId = PortalUserHolder.get();
        portalProfileService.updateEducation(studentId, id, education);
        return AjaxResult.success("教育经历更新成功");
    }

    /**
     * 删除教育经历
     */
    @DeleteMapping("/education/{id}")
    public AjaxResult deleteEducation(@PathVariable Long id) {
        Long studentId = PortalUserHolder.get();
        portalProfileService.deleteEducation(studentId, id);
        return AjaxResult.success("教育经历删除成功");
    }

    // ────────── 实习/项目经历（S-006） ──────────

    /**
     * 获取实习/项目经历列表
     */
    @GetMapping("/internship")
    public AjaxResult getInternships() {
        Long studentId = PortalUserHolder.get();
        List<StudentInternship> list = portalProfileService.getInternships(studentId);
        return AjaxResult.success(list);
    }

    /**
     * 新增实习/项目经历
     */
    @PostMapping("/internship")
    public AjaxResult addInternship(@RequestBody StudentInternship entity) {
        Long studentId = PortalUserHolder.get();
        StudentInternship saved = portalProfileService.addInternship(studentId, entity);
        return AjaxResult.success("实习/项目经历添加成功", saved);
    }

    /**
     * 更新实习/项目经历
     */
    @PutMapping("/internship/{id}")
    public AjaxResult updateInternship(@PathVariable Long id, @RequestBody StudentInternship entity) {
        Long studentId = PortalUserHolder.get();
        portalProfileService.updateInternship(studentId, id, entity);
        return AjaxResult.success("实习/项目经历更新成功");
    }

    /**
     * 删除实习/项目经历
     */
    @DeleteMapping("/internship/{id}")
    public AjaxResult deleteInternship(@PathVariable Long id) {
        Long studentId = PortalUserHolder.get();
        portalProfileService.deleteInternship(studentId, id);
        return AjaxResult.success("实习/项目经历删除成功");
    }

    // ────────── 技能/证书（S-008） ──────────

    /**
     * 获取技能/证书列表
     */
    @GetMapping("/certificate")
    public AjaxResult getCertificates() {
        Long studentId = PortalUserHolder.get();
        List<StudentCertificate> list = portalProfileService.getCertificates(studentId);
        return AjaxResult.success(list);
    }

    /**
     * 新增技能/证书
     */
    @PostMapping("/certificate")
    public AjaxResult addCertificate(@RequestBody StudentCertificate entity) {
        Long studentId = PortalUserHolder.get();
        StudentCertificate saved = portalProfileService.addCertificate(studentId, entity);
        return AjaxResult.success("技能/证书添加成功", saved);
    }

    /**
     * 更新技能/证书
     */
    @PutMapping("/certificate/{id}")
    public AjaxResult updateCertificate(@PathVariable Long id, @RequestBody StudentCertificate entity) {
        Long studentId = PortalUserHolder.get();
        portalProfileService.updateCertificate(studentId, id, entity);
        return AjaxResult.success("技能/证书更新成功");
    }

    /**
     * 删除技能/证书
     */
    @DeleteMapping("/certificate/{id}")
    public AjaxResult deleteCertificate(@PathVariable Long id) {
        Long studentId = PortalUserHolder.get();
        portalProfileService.deleteCertificate(studentId, id);
        return AjaxResult.success("技能/证书删除成功");
    }

    // ────────── 社团经历（S-009） ──────────

    /**
     * 获取社团经历列表
     */
    @GetMapping("/activity")
    public AjaxResult getActivities() {
        Long studentId = PortalUserHolder.get();
        List<StudentActivity> list = portalProfileService.getActivities(studentId);
        return AjaxResult.success(list);
    }

    /**
     * 新增社团经历
     */
    @PostMapping("/activity")
    public AjaxResult addActivity(@RequestBody StudentActivity entity) {
        Long studentId = PortalUserHolder.get();
        StudentActivity saved = portalProfileService.addActivity(studentId, entity);
        return AjaxResult.success("社团经历添加成功", saved);
    }

    /**
     * 更新社团经历
     */
    @PutMapping("/activity/{id}")
    public AjaxResult updateActivity(@PathVariable Long id, @RequestBody StudentActivity entity) {
        Long studentId = PortalUserHolder.get();
        portalProfileService.updateActivity(studentId, id, entity);
        return AjaxResult.success("社团经历更新成功");
    }

    /**
     * 删除社团经历
     */
    @DeleteMapping("/activity/{id}")
    public AjaxResult deleteActivity(@PathVariable Long id) {
        Long studentId = PortalUserHolder.get();
        portalProfileService.deleteActivity(studentId, id);
        return AjaxResult.success("社团经历删除成功");
    }

    // ────────── 个人权利中心（个保法） ──────────

    /**
     * 导出个人数据（个保法第15条：查阅权+数据可携带权）
     * <p>返回学生全部 profile+educations+files+applications+account 数据的 JSON</p>
     */
    @GetMapping("/export")
    public AjaxResult exportPersonalData() {
        Long studentId = PortalUserHolder.get();
        Map<String, Object> data = portalProfileService.exportPersonalData(studentId);
        return AjaxResult.success(data);
    }

    /**
     * 撤回隐私同意（个保法第45条）
     * <p>撤回后数据将在保留期满后自动删除</p>
     */
    @PostMapping("/withdraw-consent")
    public AjaxResult withdrawConsent() {
        Long studentId = PortalUserHolder.get();
        portalProfileService.withdrawConsent(studentId);
        return AjaxResult.success("同意已撤回，您的数据将在保留期满后删除");
    }

    /**
     * 注销账号（个保法第47条：删除权）
     * <p>设置 status=DELETED，清除个人标识字段，保留统计维度</p>
     */
    @DeleteMapping("/account")
    public AjaxResult deleteAccount() {
        Long studentId = PortalUserHolder.get();
        portalProfileService.deleteAccount(studentId);
        PortalUserHolder.clear();
        return AjaxResult.success("账号已注销");
    }
}
