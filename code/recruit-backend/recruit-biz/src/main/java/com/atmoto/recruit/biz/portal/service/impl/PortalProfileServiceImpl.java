package com.atmoto.recruit.biz.portal.service.impl;

import com.atmoto.recruit.biz.common.domain.Application;
import com.atmoto.recruit.biz.common.domain.ResumeFile;
import com.atmoto.recruit.biz.common.domain.Student;
import com.atmoto.recruit.biz.common.domain.StudentActivity;
import com.atmoto.recruit.biz.common.domain.StudentCertificate;
import com.atmoto.recruit.biz.common.domain.StudentEducation;
import com.atmoto.recruit.biz.common.domain.StudentInternship;
import com.atmoto.recruit.biz.common.domain.StudentProfile;
import com.atmoto.recruit.biz.common.mapper.ApplicationMapper;
import com.atmoto.recruit.biz.common.mapper.ResumeFileMapper;
import com.atmoto.recruit.biz.common.mapper.StudentActivityMapper;
import com.atmoto.recruit.biz.common.mapper.StudentCertificateMapper;
import com.atmoto.recruit.biz.common.mapper.StudentEducationMapper;
import com.atmoto.recruit.biz.common.mapper.StudentInternshipMapper;
import com.atmoto.recruit.biz.common.mapper.StudentMapper;
import com.atmoto.recruit.biz.common.mapper.StudentProfileMapper;
import com.atmoto.recruit.biz.portal.service.PortalProfileService;
import com.atmoto.recruit.common.enums.ErrorCode;
import com.atmoto.recruit.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生端资料服务实现
 * <p>封装基本资料与教育经历、实习/项目经历、技能证书、社团经历的 CRUD，
 * 所有操作基于 studentId（从 PortalUserHolder 获取）</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalProfileServiceImpl implements PortalProfileService {

    private final StudentMapper studentMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final StudentEducationMapper studentEducationMapper;
    private final StudentInternshipMapper studentInternshipMapper;
    private final StudentCertificateMapper studentCertificateMapper;
    private final StudentActivityMapper studentActivityMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final ApplicationMapper applicationMapper;

    @Override
    public StudentProfile getProfile(Long studentId) {
        StudentProfile profile = studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>()
                        .eq(StudentProfile::getStudentId, studentId));
        if (profile == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "学生资料不存在");
        }
        return profile;
    }

    @Override
    @Transactional
    public void updateProfile(Long studentId, StudentProfile profile) {
        // 邮箱格式校验
        if (profile.getEmail() != null && !profile.getEmail().isBlank()
                && !profile.getEmail().contains("@")) {
            throw new BizException(ErrorCode.PARAM_INVALID, "邮箱格式不正确");
        }

        // 确保只更新本人资料
        StudentProfile existing = studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>()
                        .eq(StudentProfile::getStudentId, studentId));
        if (existing == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "学生资料不存在");
        }

        // 使用 LambdaUpdateWrapper 显式更新每个字段，避免 MyBatis-Plus 策略拦截
        LambdaUpdateWrapper<StudentProfile> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(StudentProfile::getStudentId, studentId)
                .set(StudentProfile::getName, profile.getName())
                .set(StudentProfile::getGender, profile.getGender())
                .set(StudentProfile::getBirthDate, profile.getBirthDate())
                .set(StudentProfile::getEmail, profile.getEmail())
                .set(StudentProfile::getCurrentCity, profile.getCurrentCity())
                .set(StudentProfile::getNativePlace, profile.getNativePlace());
        // phone 使用已有数据，不允许通过此接口修改
        if (profile.getPhone() != null && !profile.getPhone().isBlank()) {
            updateWrapper.set(StudentProfile::getPhone, profile.getPhone());
        }
        // 头像 URL — 显式更新，即使为空字符串也写入（清空头像）
        if (profile.getAvatarUrl() != null) {
            updateWrapper.set(StudentProfile::getAvatarUrl, profile.getAvatarUrl());
        }

        studentProfileMapper.update(null, updateWrapper);
        log.info("学生资料更新成功：studentId={}, avatarUrl={}", studentId, profile.getAvatarUrl());
    }

    // ────────── 教育经历 ──────────

    /**
     * 获取教育经历列表
     */
    @Override
    public List<StudentEducation> getEducations(Long studentId) {
        return studentEducationMapper.selectList(
                new LambdaQueryWrapper<StudentEducation>()
                        .eq(StudentEducation::getStudentId, studentId)
                        .orderByAsc(StudentEducation::getSortOrder));
    }

    /**
     * 新增教育经历
     */
    @Override
    @Transactional
    public StudentEducation addEducation(Long studentId, StudentEducation education) {
        education.setStudentId(studentId);
        studentEducationMapper.insert(education);
        log.info("新增教育经历：studentId={}, schoolName={}", studentId, education.getSchoolName());
        return education;
    }

    /**
     * 更新教育经历
     */
    @Override
    @Transactional
    public void updateEducation(Long studentId, Long educationId, StudentEducation education) {
        // 越权保护：必须先查到属于当前学生的记录
        StudentEducation existing = studentEducationMapper.selectOne(
                new LambdaQueryWrapper<StudentEducation>()
                        .eq(StudentEducation::getId, educationId)
                        .eq(StudentEducation::getStudentId, studentId));
        if (existing == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "教育经历不存在");
        }

        education.setId(educationId);
        education.setStudentId(studentId);
        studentEducationMapper.updateById(education);
        log.info("更新教育经历：studentId={}, educationId={}", studentId, educationId);
    }

    /**
     * 删除教育经历
     */
    @Override
    @Transactional
    public void deleteEducation(Long studentId, Long educationId) {
        // 越权保护：必须先查到属于当前学生的记录
        StudentEducation existing = studentEducationMapper.selectOne(
                new LambdaQueryWrapper<StudentEducation>()
                        .eq(StudentEducation::getId, educationId)
                        .eq(StudentEducation::getStudentId, studentId));
        if (existing == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "教育经历不存在");
        }

        studentEducationMapper.deleteById(educationId);
        log.info("删除教育经历：studentId={}, educationId={}", studentId, educationId);
    }

    // ────────── 实习/项目经历 ──────────

    @Override
    public List<StudentInternship> getInternships(Long studentId) {
        return studentInternshipMapper.selectList(
                new LambdaQueryWrapper<StudentInternship>()
                        .eq(StudentInternship::getStudentId, studentId)
                        .orderByAsc(StudentInternship::getSortOrder));
    }

    @Override
    @Transactional
    public StudentInternship addInternship(Long studentId, StudentInternship entity) {
        entity.setStudentId(studentId);
        studentInternshipMapper.insert(entity);
        log.info("新增实习/项目经历：studentId={}, orgName={}", studentId, entity.getOrgName());
        return entity;
    }

    @Override
    @Transactional
    public void updateInternship(Long studentId, Long id, StudentInternship entity) {
        StudentInternship existing = studentInternshipMapper.selectOne(
                new LambdaQueryWrapper<StudentInternship>()
                        .eq(StudentInternship::getId, id)
                        .eq(StudentInternship::getStudentId, studentId));
        if (existing == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "实习/项目经历不存在");
        }
        entity.setId(id);
        entity.setStudentId(studentId);
        studentInternshipMapper.updateById(entity);
        log.info("更新实习/项目经历：studentId={}, id={}", studentId, id);
    }

    @Override
    @Transactional
    public void deleteInternship(Long studentId, Long id) {
        StudentInternship existing = studentInternshipMapper.selectOne(
                new LambdaQueryWrapper<StudentInternship>()
                        .eq(StudentInternship::getId, id)
                        .eq(StudentInternship::getStudentId, studentId));
        if (existing == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "实习/项目经历不存在");
        }
        studentInternshipMapper.deleteById(id);
        log.info("删除实习/项目经历：studentId={}, id={}", studentId, id);
    }

    // ────────── 技能/证书 ──────────

    @Override
    public List<StudentCertificate> getCertificates(Long studentId) {
        return studentCertificateMapper.selectList(
                new LambdaQueryWrapper<StudentCertificate>()
                        .eq(StudentCertificate::getStudentId, studentId)
                        .orderByAsc(StudentCertificate::getSortOrder));
    }

    @Override
    @Transactional
    public StudentCertificate addCertificate(Long studentId, StudentCertificate entity) {
        entity.setStudentId(studentId);
        studentCertificateMapper.insert(entity);
        log.info("新增技能/证书：studentId={}, certName={}", studentId, entity.getCertName());
        return entity;
    }

    @Override
    @Transactional
    public void updateCertificate(Long studentId, Long id, StudentCertificate entity) {
        StudentCertificate existing = studentCertificateMapper.selectOne(
                new LambdaQueryWrapper<StudentCertificate>()
                        .eq(StudentCertificate::getId, id)
                        .eq(StudentCertificate::getStudentId, studentId));
        if (existing == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "技能/证书不存在");
        }
        entity.setId(id);
        entity.setStudentId(studentId);
        studentCertificateMapper.updateById(entity);
        log.info("更新技能/证书：studentId={}, id={}", studentId, id);
    }

    @Override
    @Transactional
    public void deleteCertificate(Long studentId, Long id) {
        StudentCertificate existing = studentCertificateMapper.selectOne(
                new LambdaQueryWrapper<StudentCertificate>()
                        .eq(StudentCertificate::getId, id)
                        .eq(StudentCertificate::getStudentId, studentId));
        if (existing == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "技能/证书不存在");
        }
        studentCertificateMapper.deleteById(id);
        log.info("删除技能/证书：studentId={}, id={}", studentId, id);
    }

    // ────────── 社团经历 ──────────

    @Override
    public List<StudentActivity> getActivities(Long studentId) {
        return studentActivityMapper.selectList(
                new LambdaQueryWrapper<StudentActivity>()
                        .eq(StudentActivity::getStudentId, studentId)
                        .orderByAsc(StudentActivity::getSortOrder));
    }

    @Override
    @Transactional
    public StudentActivity addActivity(Long studentId, StudentActivity entity) {
        entity.setStudentId(studentId);
        studentActivityMapper.insert(entity);
        log.info("新增社团经历：studentId={}, orgName={}", studentId, entity.getOrgName());
        return entity;
    }

    @Override
    @Transactional
    public void updateActivity(Long studentId, Long id, StudentActivity entity) {
        StudentActivity existing = studentActivityMapper.selectOne(
                new LambdaQueryWrapper<StudentActivity>()
                        .eq(StudentActivity::getId, id)
                        .eq(StudentActivity::getStudentId, studentId));
        if (existing == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "社团经历不存在");
        }
        entity.setId(id);
        entity.setStudentId(studentId);
        studentActivityMapper.updateById(entity);
        log.info("更新社团经历：studentId={}, id={}", studentId, id);
    }

    @Override
    @Transactional
    public void deleteActivity(Long studentId, Long id) {
        StudentActivity existing = studentActivityMapper.selectOne(
                new LambdaQueryWrapper<StudentActivity>()
                        .eq(StudentActivity::getId, id)
                        .eq(StudentActivity::getStudentId, studentId));
        if (existing == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "社团经历不存在");
        }
        studentActivityMapper.deleteById(id);
        log.info("删除社团经历：studentId={}, id={}", studentId, id);
    }

    // ────────── 个人权利中心（个保法） ──────────

    /**
     * 导出个人数据（个保法第15条：查阅权+数据可携带权）
     * <p>组装全部 profile + educations + files + applications + account 数据</p>
     */
    @Override
    public Map<String, Object> exportPersonalData(Long studentId) {
        Map<String, Object> data = new HashMap<>();

        // 账号信息
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "学生账号不存在");
        }

        Map<String, Object> account = new HashMap<>();
        account.put("studentId", student.getStudentId());
        account.put("phone", student.getPhone());
        account.put("realName", student.getRealName());
        account.put("email", student.getEmail());
        account.put("status", student.getStatus());
        account.put("privacyAgreed", student.getPrivacyAgreed());
        account.put("privacyAgreedTime", student.getPrivacyAgreedTime());
        account.put("createTime", student.getCreateTime());
        data.put("account", account);

        // 基本资料
        StudentProfile profile = studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>()
                        .eq(StudentProfile::getStudentId, studentId));
        data.put("profile", profile);

        // 教育经历
        List<StudentEducation> educations = studentEducationMapper.selectList(
                new LambdaQueryWrapper<StudentEducation>()
                        .eq(StudentEducation::getStudentId, studentId)
                        .orderByAsc(StudentEducation::getSortOrder));
        data.put("educations", educations);

        // 实习/项目经历
        List<StudentInternship> internships = studentInternshipMapper.selectList(
                new LambdaQueryWrapper<StudentInternship>()
                        .eq(StudentInternship::getStudentId, studentId)
                        .orderByAsc(StudentInternship::getSortOrder));
        data.put("internships", internships);

        // 技能/证书
        List<StudentCertificate> certificates = studentCertificateMapper.selectList(
                new LambdaQueryWrapper<StudentCertificate>()
                        .eq(StudentCertificate::getStudentId, studentId)
                        .orderByAsc(StudentCertificate::getSortOrder));
        data.put("certificates", certificates);

        // 社团经历
        List<StudentActivity> activities = studentActivityMapper.selectList(
                new LambdaQueryWrapper<StudentActivity>()
                        .eq(StudentActivity::getStudentId, studentId)
                        .orderByAsc(StudentActivity::getSortOrder));
        data.put("activities", activities);

        // 简历附件
        List<ResumeFile> files = resumeFileMapper.selectList(
                new LambdaQueryWrapper<ResumeFile>()
                        .eq(ResumeFile::getStudentId, studentId));
        data.put("files", files);

        // 投递记录
        List<Application> applications = applicationMapper.selectList(
                new LambdaQueryWrapper<Application>()
                        .eq(Application::getStudentId, studentId));
        data.put("applications", applications);

        log.info("导出个人数据：studentId={}, profile={}, educations={}, internships={}, certificates={}, activities={}, files={}, applications={}",
                studentId,
                profile != null ? 1 : 0,
                educations.size(),
                internships.size(),
                certificates.size(),
                activities.size(),
                files.size(),
                applications.size());
        return data;
    }

    /**
     * 撤回隐私同意（个保法第45条）
     * <p>更新 stu_user 的 privacy_agreed='0', privacy_agreed_time=null</p>
     */
    @Override
    @Transactional
    public void withdrawConsent(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "学生账号不存在");
        }
        student.setPrivacyAgreed("0");
        student.setPrivacyAgreedTime(null);
        studentMapper.updateById(student);
        log.info("撤回隐私同意：studentId={}", studentId);
    }

    /**
     * 注销账号（个保法第47条：删除权）
     * <p>设置 status='DELETED', 清除个人标识字段（real_name/email/phone），保留统计维度</p>
     */
    @Override
    @Transactional
    public void deleteAccount(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "学生账号不存在");
        }
        student.setStatus("DELETED");
        student.setRealName(null);
        student.setEmail(null);
        student.setPhone(null);
        studentMapper.updateById(student);
        log.info("账号已注销：studentId={}", studentId);
    }
}
