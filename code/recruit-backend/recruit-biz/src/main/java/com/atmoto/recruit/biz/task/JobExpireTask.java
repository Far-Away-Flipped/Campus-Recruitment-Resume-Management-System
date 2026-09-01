package com.atmoto.recruit.biz.task;

import com.atmoto.recruit.biz.common.domain.JobPosition;
import com.atmoto.recruit.biz.common.enums.JobStatus;
import com.atmoto.recruit.biz.common.mapper.JobPositionMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 岗位到期自动下架定时任务
 * <p>每 5 分钟扫描一次，将已过截止日期的已发布岗位自动标记为已过期</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobExpireTask {

    private final JobPositionMapper jobPositionMapper;

    /**
     * 定时任务已停用：岗位过期状态改为实时派生（JobStatusResolver），不再持久化 EXPIRED。
     * 若后续需要定时清理/统计，可在此恢复调度。
     */
    // @Scheduled(cron = "0 */5 * * * ?")
    public void autoOfflineExpiredJobs() {
        // 批量 UPDATE status='EXPIRED' WHERE status='PUBLISHED' AND deadline < NOW()
        LambdaUpdateWrapper<JobPosition> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(JobPosition::getStatus, JobStatus.EXPIRED.getCode())
                .eq(JobPosition::getStatus, JobStatus.PUBLISHED.getCode())
                .lt(JobPosition::getDeadline, LocalDateTime.now());

        int count = jobPositionMapper.update(null, updateWrapper);

        if (count > 0) {
            log.info("定时任务-岗位到期自动标记过期：共标记 {} 个过期岗位", count);
        }
    }
}
