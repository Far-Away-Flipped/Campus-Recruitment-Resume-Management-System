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
     * 每 5 分钟执行一次：自动将过期的已发布岗位设为 EXPIRED（区分手动下架的 CLOSED）
     */
    @Scheduled(cron = "0 */5 * * * ?")
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
