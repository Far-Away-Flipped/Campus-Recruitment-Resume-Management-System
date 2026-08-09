package com.atmoto.recruit.biz.task;

import com.atmoto.recruit.biz.common.domain.ResumeFile;
import com.atmoto.recruit.biz.common.mapper.ResumeFileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档转换失败重试扫描定时任务
 * <p>每2分钟扫描一次，将处于 PENDING 或 FAILED 状态且重试次数未超限的简历文件标记重新转换</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversionRetryTask {

    private final ResumeFileMapper resumeFileMapper;

    /** 每2分钟扫描一次 */
    @Scheduled(cron = "0 */2 * * * ?")
    public void retryFailedConversions() {
        LambdaQueryWrapper<ResumeFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeFile::getDelFlag, "0")
               .and(w -> w
                   .eq(ResumeFile::getPreviewStatus, "FAILED")
                   .or()
                   .eq(ResumeFile::getPreviewStatus, "PENDING"))
               .eq(ResumeFile::getFileExt, "DOCX");

        List<ResumeFile> files = resumeFileMapper.selectList(wrapper);
        if (files.isEmpty()) {
            return;
        }

        int retried = 0;
        for (ResumeFile file : files) {
            if ("FAILED".equals(file.getPreviewStatus())) {
                log.info("重试转换：fileId={}, fileName={}", file.getId(), file.getOriginalName());
            }
            ResumeFile update = new ResumeFile();
            update.setId(file.getId());
            update.setPreviewStatus("PENDING");
            update.setPreviewError(null);
            resumeFileMapper.updateById(update);
            retried++;
        }

        if (retried > 0) {
            log.info("转换重试扫描完成：标记 {} 个文件为待转换", retried);
        }
    }
}
