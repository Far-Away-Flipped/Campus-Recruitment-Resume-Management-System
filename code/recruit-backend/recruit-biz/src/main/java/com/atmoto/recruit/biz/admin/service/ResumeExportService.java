package com.atmoto.recruit.biz.admin.service;

import java.util.List;

/**
 * 简历导出服务接口
 * <p>导出简历列表为Excel，含日累计上限、水印、审计留痕</p>
 *
 * @author atmoto-recruit
 */
public interface ResumeExportService {

    /**
     * 导出简历列表为Excel文件
     * <p>
     * - 日累计上限：2000条（用Caffeine计数）
     * - 导出字段：姓名、学校、专业、学历、手机号(脱敏)、投递时间、当前状态
     * - 三处水印："遨天科技校园招聘 内部资料 {operatorName} {date}"
     * - 审计留痕：记录导出操作到audit_resume_access
     * </p>
     *
     * @param applicationIds 要导出的投递记录ID列表
     * @param operatorUserId 操作HR的用户ID
     * @param operatorName   操作HR的用户名
     * @param hasAllDataScope 是否拥有全部数据权限
     * @return 生成的文件路径
     */
    String exportResumeList(List<Long> applicationIds, Long operatorUserId,
                            String operatorName, boolean hasAllDataScope);
}
