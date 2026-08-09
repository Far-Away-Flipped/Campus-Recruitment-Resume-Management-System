package com.atmoto.recruit.biz.admin.service.impl;

import com.atmoto.recruit.biz.admin.service.ReportService;
import com.atmoto.recruit.biz.admin.vo.ReportDataVO;
import com.atmoto.recruit.biz.admin.vo.TrendReportVO;
import com.atmoto.recruit.biz.common.mapper.ApplicationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 报表服务实现
 * <p>所有报表查询均含数据范围约束（ownerUserId），一期口径仅到筛选环节</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ApplicationMapper applicationMapper;

    @Override
    public List<TrendReportVO> getApplyTrend(LocalDate startDate, LocalDate endDate, Long ownerUserId) {
        List<TrendReportVO> result = applicationMapper.countByApplyDate(startDate, endDate, ownerUserId);
        log.debug("报表-投递趋势：startDate={}, endDate={}, 结果条数={}", startDate, endDate, result.size());
        return result;
    }

    @Override
    public List<ReportDataVO> getJobRanking(int topN, Long ownerUserId) {
        List<ReportDataVO> result = applicationMapper.countByJob(topN, ownerUserId);
        log.debug("报表-岗位排行：topN={}, 结果条数={}", topN, result.size());
        return result;
    }

    @Override
    public List<ReportDataVO> getSchoolDistribution(Long ownerUserId) {
        List<ReportDataVO> result = applicationMapper.countBySnapshotSchool(ownerUserId);
        log.debug("报表-学校分布：结果条数={}", result.size());
        return result;
    }

    @Override
    public List<ReportDataVO> getDegreeDistribution(Long ownerUserId) {
        List<ReportDataVO> result = applicationMapper.countBySnapshotDegree(ownerUserId);
        log.debug("报表-学历分布：结果条数={}", result.size());
        return result;
    }

    @Override
    public List<ReportDataVO> getSourceDistribution(LocalDate startDate, LocalDate endDate, Long ownerUserId) {
        List<ReportDataVO> result = applicationMapper.countBySource(startDate, endDate, ownerUserId);
        log.debug("报表-渠道来源分布：startDate={}, endDate={}, 结果条数={}", startDate, endDate, result.size());
        return result;
    }
}
