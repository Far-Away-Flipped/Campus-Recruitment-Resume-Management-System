package com.atmoto.recruit.biz.common.service;

import com.atmoto.recruit.biz.common.domain.Application;

/**
 * 投递核心 Service 接口（M4 投递流程）
 * <p>管理投递记录的创建、版本管理，含完整校验链与快照生成</p>
 *
 * @author atmoto-recruit
 */
public interface ApplicationService {

    /**
     * 学生投递岗位（核心事务）
     * <p>
     * 校验链（按顺序）：
     * <ol>
     *   <li>岗位存在且 status=PUBLISHED 且 deadline >= 今天</li>
     *   <li>学生资料完整：stu_profile存在 + stu_education至少一条 + stu_resume_file至少一条</li>
     *   <li>未重复投递：uk_student_job 唯一约束（DuplicateKeyException 全局处理）</li>
     *   <li>乐观锁：version 字段防并发</li>
     * </ol>
     * 事务内操作：
     * <ol>
     *   <li>生成简历快照 → INSERT app_snapshot</li>
     *   <li>INSERT app_application（status=PENDING_SCREEN）</li>
     *   <li>INSERT app_status_history（from_status=null, to_status=PENDING_SCREEN）</li>
     *   <li>UPDATE job_position SET application_count+1</li>
     *   <li>异步发送站内信通知（学生 + HR）</li>
     * </ol>
     * </p>
     *
     * @param studentId 学生ID（来自 PortalUserHolder）
     * @param jobId     目标岗位ID
     * @param source       渠道来源（默认 OFFICIAL_SITE）
     * @param sourceDetail 渠道详情/推荐人（内推渠道填推荐人姓名，可空）
     * @param fileId       指定用作简历快照的附件ID（可空，不传则默认取第一条）
     * @return 投递记录
     */
    Application submitApplication(Long studentId, Long jobId, String source, String sourceDetail, Long fileId);
}
