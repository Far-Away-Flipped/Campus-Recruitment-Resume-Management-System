package com.atmoto.recruit.biz.task;

import com.atmoto.recruit.biz.common.domain.JobPosition;
import com.atmoto.recruit.biz.common.enums.JobStatus;
import com.atmoto.recruit.biz.common.mapper.JobPositionMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 岗位到期自动下架定时任务
 * <p>每 5 分钟扫描一次，将已过截止日期的已发布岗位自动下架</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobExpireTask {

    private final JobPositionMapper jobPositionMapper;

    /**
     * 每 5 分钟执行一次：自动下架过期的已发布岗位
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void autoOfflineExpiredJobs() {
        // 查询 status='PUBLISHED' 且 deadline < NOW() 的岗位
        // ★ 使用 DB 层 NOW() 比较，保证 DATETIME 精度（而非 Java LocalDate）
        QueryWrapper<JobPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("status", JobStatus.PUBLISHED.getCode())
                .apply("deadline < NOW()");

        List<JobPosition> expiredJobs = jobPositionMapper.selectList(wrapper);

        if (expiredJobs.isEmpty()) {
            return;
        }

        // 批量更新 status='CLOSED'
        int count = 0;
        for (JobPosition job : expiredJobs) {
            JobPosition update = new JobPosition();
            update.setJobId(job.getJobId());
            update.setStatus(JobStatus.CLOSED.getCode());
            jobPositionMapper.updateById(update);
            count++;
        }

        log.info("定时任务-岗位到期自动下架：共下架 {} 个到期岗位（共扫描 {} 个过期岗位）",
                count, expiredJobs.size());
    }
}
