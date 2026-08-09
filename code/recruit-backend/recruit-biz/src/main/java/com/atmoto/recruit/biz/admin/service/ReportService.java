package com.atmoto.recruit.biz.admin.service;

import com.atmoto.recruit.biz.admin.vo.ReportDataVO;
import com.atmoto.recruit.biz.admin.vo.TrendReportVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 报表服务接口
 * <p>提供HR端数据统计报表：投递趋势、岗位排行、学校分布、学历分布、渠道来源分布。
 * 一期口径仅到筛选环节，所有报表查询均含数据范围约束。</p>
 *
 * @author atmoto-recruit
 */
public interface ReportService {

    /**
     * 投递总量趋势：按日期分组统计每日投递数
     *
     * @param startDate   起始日期（可空）
     * @param endDate     截止日期（可空）
     * @param ownerUserId 数据范围限制（可空=全部权限）
     * @return 按日期排序的每日投递数列表
     */
    List<TrendReportVO> getApplyTrend(LocalDate startDate, LocalDate endDate, Long ownerUserId);

    /**
     * 岗位投递排行：按岗位分组统计投递数并降序排列
     *
     * @param topN        返回前N条
     * @param ownerUserId 数据范围限制（可空=全部权限）
     * @return 岗位名+投递数的降序列表
     */
    List<ReportDataVO> getJobRanking(int topN, Long ownerUserId);

    /**
     * 学校分布：按快照学校字段分组统计投递数
     *
     * @param ownerUserId 数据范围限制（可空=全部权限）
     * @return 学校名+投递数的列表
     */
    List<ReportDataVO> getSchoolDistribution(Long ownerUserId);

    /**
     * 学历分布：按快照学历字段分组统计投递数
     *
     * @param ownerUserId 数据范围限制（可空=全部权限）
     * @return 学历+投递数的列表
     */
    List<ReportDataVO> getDegreeDistribution(Long ownerUserId);

    /**
     * 渠道来源分布：按 source 字段分组统计投递数
     *
     * @param startDate   起始日期（可空）
     * @param endDate     截止日期（可空）
     * @param ownerUserId 数据范围限制（可空=全部权限）
     * @return 渠道来源+投递数的列表
     */
    List<ReportDataVO> getSourceDistribution(LocalDate startDate, LocalDate endDate, Long ownerUserId);
}
