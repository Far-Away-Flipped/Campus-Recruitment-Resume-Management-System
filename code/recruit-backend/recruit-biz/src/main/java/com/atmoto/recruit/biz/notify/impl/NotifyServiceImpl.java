package com.atmoto.recruit.biz.notify.impl;

import com.atmoto.recruit.biz.common.domain.JobPosition;
import com.atmoto.recruit.biz.common.domain.NotMessage;
import com.atmoto.recruit.biz.common.mapper.JobPositionMapper;
import com.atmoto.recruit.biz.common.mapper.NotMessageMapper;
import com.atmoto.recruit.biz.notify.NotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 站内信通知 Service 实现（S-015 P1）
 * <p>将站内信消息写入 not_message 表，支持学生端和HR端查询与已读标记。</p>
 *
 * @author atmoto-recruit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    private final JobPositionMapper jobPositionMapper;
    private final NotMessageMapper notMessageMapper;

    @Async
    @Override
    public void sendInAppMessage(Long receiverStudentId, String title, String content) {
        NotMessage msg = new NotMessage();
        msg.setRecipientId(receiverStudentId);
        msg.setRecipientType("STUDENT");
        msg.setTitle(title);
        msg.setContent(content);
        msg.setMessageType("APPLY_SUCCESS");
        msg.setIsRead("0");
        msg.setCreateTime(LocalDateTime.now());
        notMessageMapper.insert(msg);
        log.info("【站内信-学生】接收人studentId={}, 标题={}, 消息ID={}",
                receiverStudentId, title, msg.getId());
    }

    @Async
    @Override
    public void sendHrNotification(Long jobId, String title, String content) {
        // 查询岗位负责人
        JobPosition job = jobPositionMapper.selectById(jobId);
        if (job == null || job.getOwnerUserId() == null) {
            log.warn("【HR通知】岗位{}无负责人，跳过通知。标题={}", jobId, title);
            return;
        }
        Long ownerUserId = job.getOwnerUserId();

        NotMessage msg = new NotMessage();
        msg.setRecipientId(ownerUserId);
        msg.setRecipientType("HR");
        msg.setTitle(title);
        msg.setContent(content);
        msg.setMessageType("NEW_APPLICATION");
        msg.setIsRead("0");
        msg.setRefId(jobId);
        msg.setCreateTime(LocalDateTime.now());
        notMessageMapper.insert(msg);
        log.info("【站内信-HR】接收人ownerUserId={}, 岗位jobId={}, 标题={}, 消息ID={}",
                ownerUserId, jobId, title, msg.getId());
    }

    /**
     * 发送简历状态变更站内信（同步，不加 @Async）
     * <p>调用点在事务上下文内，必须同步执行避免读到未提交状态；用 dedup_key 唯一索引防重。</p>
     */
    @Override
    public void sendStatusChangeNotice(Long receiverStudentId, Long applicationId,
                                       Long jobId, String statusLabel, Long historyId) {
        // 查询岗位名，为空时回退「该岗位」
        String jobTitle = "该岗位";
        if (jobId != null) {
            JobPosition job = jobPositionMapper.selectById(jobId);
            if (job != null && job.getTitle() != null && !job.getTitle().isBlank()) {
                jobTitle = job.getTitle();
            }
        }
        NotMessage msg = new NotMessage();
        msg.setRecipientId(receiverStudentId);
        msg.setRecipientType("STUDENT");
        msg.setMessageType("APPLICATION_STATUS_CHANGED");
        msg.setTitle("投递进度更新");
        msg.setContent("您投递的【" + jobTitle + "】岗位简历状态已更新为：「" + statusLabel + "」");
        msg.setRefId(applicationId);
        msg.setDedupKey("APPLICATION_STATUS_CHANGED:" + historyId);
        msg.setIsRead("0");
        msg.setCreateTime(LocalDateTime.now());
        notMessageMapper.insert(msg);
        log.info("【站内信-状态变更】接收人studentId={}, applicationId={}, historyId={}, 消息ID={}",
                receiverStudentId, applicationId, historyId, msg.getId());
    }
}
