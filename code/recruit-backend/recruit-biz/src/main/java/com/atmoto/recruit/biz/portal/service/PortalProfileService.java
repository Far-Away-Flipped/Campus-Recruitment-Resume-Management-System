package com.atmoto.recruit.biz.portal.service;

import com.atmoto.recruit.biz.common.domain.StudentActivity;
import com.atmoto.recruit.biz.common.domain.StudentCertificate;
import com.atmoto.recruit.biz.common.domain.StudentEducation;
import com.atmoto.recruit.biz.common.domain.StudentInternship;
import com.atmoto.recruit.biz.common.domain.StudentProfile;

import java.util.List;
import java.util.Map;

/**
 * 学生端资料服务接口
 * <p>封装基本资料与教育经历、实习/项目经历、技能证书、社团经历的 CRUD 操作</p>
 */
public interface PortalProfileService {

    /**
     * 获取本人基本资料
     *
     * @param studentId 学生ID（从 PortalUserHolder 获取，不接受参数传入）
     * @return 学生资料
     */
    StudentProfile getProfile(Long studentId);

    /**
     * 更新基本资料
     *
     * @param studentId 学生ID（从 PortalUserHolder 获取）
     * @param profile   资料内容
     */
    void updateProfile(Long studentId, StudentProfile profile);

    // ────────── 教育经历 ──────────

    /**
     * 获取教育经历列表
     *
     * @param studentId 学生ID
     * @return 教育经历列表，按 sortOrder 升序
     */
    List<StudentEducation> getEducations(Long studentId);

    /**
     * 新增教育经历
     *
     * @param studentId  学生ID
     * @param education  教育经历
     * @return 保存后的教育经历（含自增ID）
     */
    StudentEducation addEducation(Long studentId, StudentEducation education);

    /**
     * 更新教育经历
     *
     * @param studentId    学生ID
     * @param educationId  教育经历ID
     * @param education    教育经历新内容
     */
    void updateEducation(Long studentId, Long educationId, StudentEducation education);

    /**
     * 删除教育经历
     *
     * @param studentId    学生ID
     * @param educationId  教育经历ID
     */
    void deleteEducation(Long studentId, Long educationId);

    // ────────── 实习/项目经历 ──────────

    /**
     * 获取实习/项目经历列表
     */
    List<StudentInternship> getInternships(Long studentId);

    /**
     * 新增实习/项目经历
     */
    StudentInternship addInternship(Long studentId, StudentInternship entity);

    /**
     * 更新实习/项目经历
     */
    void updateInternship(Long studentId, Long id, StudentInternship entity);

    /**
     * 删除实习/项目经历
     */
    void deleteInternship(Long studentId, Long id);

    // ────────── 技能/证书 ──────────

    /**
     * 获取技能/证书列表
     */
    List<StudentCertificate> getCertificates(Long studentId);

    /**
     * 新增技能/证书
     */
    StudentCertificate addCertificate(Long studentId, StudentCertificate entity);

    /**
     * 更新技能/证书
     */
    void updateCertificate(Long studentId, Long id, StudentCertificate entity);

    /**
     * 删除技能/证书
     */
    void deleteCertificate(Long studentId, Long id);

    // ────────── 社团经历 ──────────

    /**
     * 获取社团经历列表
     */
    List<StudentActivity> getActivities(Long studentId);

    /**
     * 新增社团经历
     */
    StudentActivity addActivity(Long studentId, StudentActivity entity);

    /**
     * 更新社团经历
     */
    void updateActivity(Long studentId, Long id, StudentActivity entity);

    /**
     * 删除社团经历
     */
    void deleteActivity(Long studentId, Long id);

    /**
     * 导出个人数据（个保法第15条：查阅权+数据可携带权）
     * <p>组装全部 profile+educations+files+applications+account 数据返回给用户</p>
     *
     * @param studentId 学生ID（从 PortalUserHolder 获取）
     * @return 个人全部数据的 Map
     */
    Map<String, Object> exportPersonalData(Long studentId);

    /**
     * 撤回隐私同意（个保法第45条）
     * <p>更新 stu_user 的 privacy_agreed='0', privacy_agreed_time=null</p>
     *
     * @param studentId 学生ID（从 PortalUserHolder 获取）
     */
    void withdrawConsent(Long studentId);

    /**
     * 注销账号（个保法第47条：删除权）
     * <p>设置 status='DELETED', 清除个人标识字段（real_name/email/phone），保留统计维度</p>
     *
     * @param studentId 学生ID（从 PortalUserHolder 获取）
     */
    void deleteAccount(Long studentId);
}
